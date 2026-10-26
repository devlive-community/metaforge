package org.devlive.metaforge.driver.iterable;

import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.Function;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Collation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressFBWarnings(value = {"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP2"})
public class InMemoryFindIterable
        implements FindIterable<Document>
{
    private final List<Document> documents;

    public InMemoryFindIterable(List<Document> documents)
    {
        this.documents = documents;
    }

    @Override
    public MongoCursor<Document> iterator()
    {
        return new InMemoryMongoCursor(documents);
    }

    @Override
    public MongoCursor<Document> cursor()
    {
        return null;
    }

    @Override
    public Document first()
    {
        return documents.isEmpty() ? null : documents.get(0);
    }

    @Override
    public <U> MongoIterable<U> map(Function<Document, U> function)
    {
        return null;
    }

    @Override
    public <A extends Collection<? super Document>> A into(A objects)
    {
        return null;
    }

    @Override
    public FindIterable<Document> filter(Bson filter)
    {
        return this;
    }

    @Override
    public FindIterable<Document> limit(int limit)
    {
        return this;
    }

    @Override
    public FindIterable<Document> skip(int skip)
    {
        return this;
    }

    @Override
    public FindIterable<Document> maxTime(long maxTime, TimeUnit timeUnit)
    {
        return this;
    }

    @Override
    public FindIterable<Document> maxAwaitTime(long l, TimeUnit timeUnit)
    {
        return null;
    }

    @Override
    public FindIterable<Document> projection(Bson bson)
    {
        return null;
    }

    @Override
    public FindIterable<Document> sort(Bson bson)
    {
        return null;
    }

    @Override
    public FindIterable<Document> noCursorTimeout(boolean b)
    {
        return null;
    }

    @Override
    public FindIterable<Document> oplogReplay(boolean b)
    {
        return null;
    }

    @Override
    public FindIterable<Document> partial(boolean b)
    {
        return null;
    }

    @Override
    public FindIterable<Document> cursorType(CursorType cursorType)
    {
        return null;
    }

    @Override
    public FindIterable<Document> batchSize(int batchSize)
    {
        return this;
    }

    @Override
    public FindIterable<Document> collation(Collation collation)
    {
        return null;
    }

    @Override
    public FindIterable<Document> comment(String s)
    {
        return null;
    }

    @Override
    public FindIterable<Document> comment(BsonValue bsonValue)
    {
        return null;
    }

    @Override
    public FindIterable<Document> hint(Bson bson)
    {
        return null;
    }

    @Override
    public FindIterable<Document> hintString(String s)
    {
        return null;
    }

    @Override
    public FindIterable<Document> let(Bson bson)
    {
        return null;
    }

    @Override
    public FindIterable<Document> max(Bson bson)
    {
        return null;
    }

    @Override
    public FindIterable<Document> min(Bson bson)
    {
        return null;
    }

    @Override
    public FindIterable<Document> returnKey(boolean b)
    {
        return null;
    }

    @Override
    public FindIterable<Document> showRecordId(boolean b)
    {
        return null;
    }

    @Override
    public FindIterable<Document> allowDiskUse(Boolean aBoolean)
    {
        return null;
    }

    @Override
    public Document explain()
    {
        return null;
    }

    @Override
    public Document explain(ExplainVerbosity explainVerbosity)
    {
        return null;
    }

    @Override
    public <E> E explain(Class<E> aClass)
    {
        return null;
    }

    @Override
    public <E> E explain(Class<E> aClass, ExplainVerbosity explainVerbosity)
    {
        return null;
    }

    private static class InMemoryMongoCursor
            implements MongoCursor<Document>
    {
        private final List<Document> documents;
        private int position = 0;

        public InMemoryMongoCursor(List<Document> documents)
        {
            this.documents = documents;
        }

        @Override
        public void close()
        {
        }

        @Override
        public boolean hasNext()
        {
            return position < documents.size();
        }

        @Override
        public Document next()
        {
            return documents.get(position++);
        }

        @Override
        public int available()
        {
            return 0;
        }

        @Override
        public Document tryNext()
        {
            return hasNext() ? next() : null;
        }

        @Override
        public ServerCursor getServerCursor()
        {
            return null;
        }

        @Override
        public ServerAddress getServerAddress()
        {
            return null;
        }
    }
}
