package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementEntry;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.redisson.Redisson;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.config.Config;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Read-only, cursor-based Redis browsing. Lua returns bounded value previews, never whole collections. */
public class RedisManagement
{
    private static final String SCAN = "local p=redis.call('SCAN',ARGV[1],'MATCH',ARGV[2],'COUNT',ARGV[3]); local r={}; "
            + "for _,k in ipairs(p[2]) do local t=redis.call('TYPE',k).ok; if t~='none' then "
            + "r[#r+1]={k,t,redis.call('PTTL',k)}; end; end; return {p[1],r}";
    private static final String DETAIL = "local k=ARGV[1]; local c=ARGV[2]; local n=tonumber(ARGV[3]); "
            + "local t=redis.call('TYPE',k).ok; local ttl=redis.call('PTTL',k); local rows={}; local size=-1; local next=''; local content=''; local cut=0; "
            + "if t=='string' then size=redis.call('STRLEN',k); content=redis.call('GETRANGE',k,0,65535); if size>65536 then cut=1 end; "
            + "elseif t=='list' then size=redis.call('LLEN',k); local start=tonumber(c); local v=redis.call('LRANGE',k,start,start+n-1); "
            + "for i,x in ipairs(v) do rows[#rows+1]={tostring(start+i-1),string.sub(x,1,2048),string.len(x)>2048 and 1 or 0}; end; if start+#v<size then next=tostring(start+#v) end; "
            + "elseif t=='zset' then size=redis.call('ZCARD',k); local start=tonumber(c); local v=redis.call('ZRANGE',k,start,start+n-1,'WITHSCORES'); "
            + "for i=1,#v,2 do rows[#rows+1]={v[i+1],string.sub(v[i],1,2048),string.len(v[i])>2048 and 1 or 0}; end; if start+#v/2<size then next=tostring(start+#v/2) end; "
            + "elseif t=='hash' then size=redis.call('HLEN',k); local p=redis.call('HSCAN',k,c,'COUNT',n); if p[1]~='0' then next=p[1] end; "
            + "for i=1,#p[2],2 do rows[#rows+1]={string.sub(p[2][i],1,4096),string.sub(p[2][i+1],1,2048),(string.len(p[2][i])>4096 or string.len(p[2][i+1])>2048) and 1 or 0}; end; "
            + "elseif t=='set' then size=redis.call('SCARD',k); local p=redis.call('SSCAN',k,c,'COUNT',n); if p[1]~='0' then next=p[1] end; "
            + "for i,x in ipairs(p[2]) do rows[#rows+1]={tostring(i),string.sub(x,1,2048),string.len(x)>2048 and 1 or 0}; end; "
            + "elseif t=='stream' then size=redis.call('XLEN',k); local v=redis.call('XRANGE',k,c=='0' and '-' or c,'+','COUNT',n+2); local last=''; "
            + "for _,item in ipairs(v) do if item[1]~=c then if #rows>=n then next=last; break; end; local fields={}; "
            + "for i=1,math.min(#item[2],40) do fields[i]=string.sub(item[2][i],1,2048); end; local value=cjson.encode(fields); "
            + "rows[#rows+1]={item[1],string.sub(value,1,2048),(#value>2048 or #item[2]>40) and 1 or 0}; last=item[1]; end; end; "
            + "end; return {t,ttl,size,content,rows,next,cut}";

    public ManagementPage namespaces(RuntimeConfigure configure)
    {
        String database = configuredDatabase(configure);
        ManagementEntry entry = new ManagementEntry();
        entry.setId(database);
        entry.setName("DB " + database);
        entry.setType("database");
        ManagementPage page = new ManagementPage();
        page.getEntries().add(entry);
        page.setNamespace(database);
        return page;
    }

    public ManagementPage list(RuntimeConfigure configure, ManagementRequest request)
    {
        String database = namespace(configure, request);
        String cursor = cursor(request.getCursor());
        String pattern = request.getPrefix().isEmpty() ? "*" : request.getPrefix();
        return withClient(configure, database, client -> {
            List<?> result = eval(client, SCAN, bytes(cursor), bytes(pattern), bytes(String.valueOf(request.getLimit())));
            ManagementPage page = new ManagementPage();
            page.setNamespace(database);
            page.setPrefix(pattern);
            String next = text(result.get(0));
            page.setHasMore(!"0".equals(next));
            page.setNextCursor(page.isHasMore() ? next : null);
            // SCAN COUNT is a hint: preserving the entire batch avoids skipping returned keys.
            for (Object raw : (List<?>) result.get(1)) {
                List<?> row = (List<?>) raw;
                byte[] key = rawBytes(row.get(0));
                ManagementEntry entry = new ManagementEntry();
                entry.setId("k:" + Base64.getUrlEncoder().withoutPadding().encodeToString(key));
                entry.setName(display(key));
                entry.setType(text(row.get(1)));
                entry.setTtl(number(row.get(2)));
                page.getEntries().add(entry);
            }
            return page;
        });
    }

    public ManagementDetail detail(RuntimeConfigure configure, ManagementRequest request)
    {
        String database = namespace(configure, request);
        byte[] key = decodeKey(request.getKey());
        String position = request.getCursor().isEmpty() ? "0" : request.getCursor();
        if (!position.matches("[0-9]{1,20}(-[0-9]{1,20})?")) {
            throw new IllegalArgumentException("Invalid Redis detail cursor");
        }
        return withClient(configure, database, client -> {
            List<?> result = eval(client, DETAIL, key, bytes(position), bytes(String.valueOf(request.getLimit())));
            String type = text(result.get(0));
            if ("none".equals(type)) {
                throw new IllegalArgumentException("The key no longer exists");
            }
            ManagementEntry entry = new ManagementEntry();
            entry.setId(request.getKey());
            entry.setName(display(key));
            entry.setType(type);
            entry.setTtl(number(result.get(1)));
            entry.setSize(number(result.get(2)));
            ManagementDetail detail = new ManagementDetail();
            detail.setEntry(entry);
            if ("string".equals(type)) {
                byte[] value = rawBytes(result.get(3));
                boolean binary = isBinary(value);
                detail.setContent(binary ? Base64.getEncoder().encodeToString(value) : text(value));
                detail.setEncoding(binary ? "base64" : "utf-8");
            }
            for (Object raw : (List<?>) result.get(4)) {
                List<?> row = (List<?>) raw;
                Map<String, String> item = new LinkedHashMap<>();
                item.put("position", display(rawBytes(row.get(0))));
                byte[] value = rawBytes(row.get(1));
                boolean binary = isBinary(value);
                item.put("value", binary ? Base64.getEncoder().encodeToString(value) : text(value));
                item.put("encoding", binary ? "base64" : "utf-8");
                item.put("truncated", String.valueOf(number(row.get(2)) == 1));
                detail.getRows().add(item);
            }
            String next = text(result.get(5));
            detail.setHasMore(!next.isEmpty());
            detail.setNextCursor(next.isEmpty() ? null : next);
            detail.setTruncated(number(result.get(6)) == 1);
            entry.getAttributes().put("previewSupported", String.valueOf(List.of("string", "list", "hash", "set", "zset", "stream").contains(type)));
            return detail;
        });
    }

    protected RedissonClient openClient(RuntimeConfigure configure, String database)
    {
        String host = configure.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Redis host is required");
        }
        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        Config config = new Config();
        config.setThreads(2).setNettyThreads(2);
        config.useSingleServer()
                .setAddress((configure.getSsl().orElse(false) ? "rediss://" : "redis://") + host + ":" + configure.getPort())
                .setDatabase(Integer.parseInt(database))
                .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4)
                .setConnectTimeout(5000).setTimeout(5000).setRetryAttempts(0);
        configure.getUsername().filter(value -> !value.isEmpty()).ifPresent(value -> config.useSingleServer().setUsername(value));
        configure.getPassword().ifPresent(value -> config.useSingleServer().setPassword(value));
        return Redisson.create(config);
    }

    private <T> T withClient(RuntimeConfigure configure, String database, Function<RedissonClient, T> action)
    {
        RedissonClient client = openClient(configure, database);
        try {
            return action.apply(client);
        }
        finally {
            client.shutdown();
        }
    }

    private List<?> eval(RedissonClient client, String script, Object... arguments)
    {
        return client.getScript(ByteArrayCodec.INSTANCE).eval(RScript.Mode.READ_ONLY, script, RScript.ReturnType.MULTI, Collections.emptyList(), arguments);
    }

    private String configuredDatabase(RuntimeConfigure configure)
    {
        return cursor(configure.getDatabase().orElse("0"));
    }

    private String namespace(RuntimeConfigure configure, ManagementRequest request)
    {
        String configured = configuredDatabase(configure);
        if (request.getNamespace() != null && !request.getNamespace().equals(configured)) {
            throw new IllegalArgumentException("This connection is configured for DB " + configured);
        }
        return configured;
    }

    private String cursor(String value)
    {
        String cursor = value == null || value.isEmpty() ? "0" : value;
        if (!cursor.matches("[0-9]{1,20}")) {
            throw new IllegalArgumentException("Invalid Redis cursor");
        }
        return cursor;
    }

    private byte[] decodeKey(String value)
    {
        if (value == null || !value.startsWith("k:")) {
            throw new IllegalArgumentException("Invalid key identifier");
        }
        try {
            return Base64.getUrlDecoder().decode(value.substring(2));
        }
        catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid key identifier", error);
        }
    }

    private byte[] bytes(String value)
    {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] rawBytes(Object value)
    {
        return value instanceof byte[] ? (byte[]) value : bytes(String.valueOf(value));
    }

    private String text(Object value)
    {
        return value instanceof byte[] ? new String((byte[]) value, StandardCharsets.UTF_8) : String.valueOf(value);
    }

    private long number(Object value)
    {
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(text(value));
    }

    private String display(byte[] value)
    {
        return isBinary(value) ? "[base64] " + Base64.getEncoder().encodeToString(value) : text(value);
    }

    private boolean isBinary(byte[] value)
    {
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value)).toString();
            return decoded.codePoints().anyMatch(character -> Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t');
        }
        catch (CharacterCodingException error) {
            return true;
        }
    }
}
