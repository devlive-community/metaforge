package org.devlive.metaforge.service.common;

import org.devlive.metaforge.service.record.TreeRecord;

import java.util.Locale;
import java.util.regex.Pattern;

public final class NavigationPaths
{
    private static final Pattern SYSTEM = Pattern.compile("^/system(?=[/?#]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADMIN_ROOT = Pattern.compile("^/admin(?=[/?#]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT = Pattern.compile("^/admin/user/(profile|info|log|editor|assistant|username|password|notify)(?=[/?#]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUSINESS = Pattern.compile("^/admin/(query|source|dataset|dashboard|history|snippet|report|chat|workflow)(?=[/?#]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADMIN = Pattern.compile("^/admin/(user|role|menu|schedule|function|configure)(?=[/?#]|$)", Pattern.CASE_INSENSITIVE);

    private NavigationPaths()
    {
    }

    public static String canonical(String path)
    {
        if (path == null) {
            return null;
        }
        String result = SYSTEM.matcher(path).replaceFirst("/admin");
        result = ADMIN_ROOT.matcher(result).replaceFirst("/admin");
        result = ACCOUNT.matcher(result).replaceFirst(match -> "/account/" + match.group(1).toLowerCase(Locale.ROOT));
        result = BUSINESS.matcher(result).replaceFirst(match -> "/" + match.group(1).toLowerCase(Locale.ROOT));
        return ADMIN.matcher(result).replaceFirst(match -> "/admin/" + match.group(1).toLowerCase(Locale.ROOT));
    }

    public static boolean administrative(String path)
    {
        String canonical = canonical(path);
        return canonical != null && ADMIN_ROOT.matcher(canonical).find();
    }

    public static TreeRecord normalize(TreeRecord menu)
    {
        if ("MANAGEMENT".equals(menu.getCode()) && ("/admin".equals(menu.getUrl()) || "common.admin".equals(menu.getI18nKey()))) {
            menu.setUrl("/workspace");
            menu.setI18nKey("navigation.dataDevelopment");
        }
        else if ("SYSTEM".equals(menu.getCode()) && ("/system".equals(menu.getUrl()) || "common.system".equals(menu.getI18nKey()))) {
            menu.setUrl("/admin");
            menu.setI18nKey("navigation.systemManagement");
        }
        else {
            menu.setUrl(canonical(menu.getUrl()));
        }
        return menu;
    }
}
