package org.devlive.metaforge.driver.iterable;

import com.mongodb.ExplainVerbosity;
import com.mongodb.Function;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Collation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuppressFBWarnings(value = {"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP2"})
public class InMemoryAggregateIterable
        implements AggregateIterable<Document>
{
    private final List<Document> documents;

    public InMemoryAggregateIterable(List<Document> documents)
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

    // Interface implementations with minimal implementation
    @Override
    public AggregateIterable<Document> batchSize(int size)
    {
        return this;
    }

    @Override
    public void toCollection()
    {}

    @Override
    public AggregateIterable<Document> allowDiskUse(Boolean allowDiskUse)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> maxTime(long maxTime, TimeUnit timeUnit)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> bypassDocumentValidation(Boolean bypassDocumentValidation)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> collation(Collation collation)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> comment(String comment)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> comment(BsonValue bsonValue)
    {
        return null;
    }

    @Override
    public AggregateIterable<Document> hint(Bson hint)
    {
        return this;
    }

    @Override
    public AggregateIterable<Document> hintString(String s)
    {
        return null;
    }

    @Override
    public AggregateIterable<Document> let(Bson bson)
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

    @Override
    public void forEach(Consumer<? super Document> action)
    {
        MongoCursor<Document> cursor = iterator();
        while (cursor.hasNext()) {
            action.accept(cursor.next());
        }
    }

    @Override
    public <A extends Collection<? super Document>> A into(A target)
    {
        forEach(target::add);
        return target;
    }

    @Override
    public Document first()
    {
        MongoCursor<Document> cursor = iterator();
        return cursor.hasNext() ? cursor.next() : null;
    }

    @Override
    public <U> MongoIterable<U> map(Function<Document, U> mapper)
    {
        throw new UnsupportedOperationException("Map operation not supported");
    }

    private static class InMemoryMongoCursor
            implements MongoCursor<Document>
    {
        private final List<Document> results;
        private int position = 0;

        public InMemoryMongoCursor(List<Document> results)
        {
            this.results = results;
        }

        @Override
        public void close()
        {
            // No resources to close
        }

        @Override
        public boolean hasNext()
        {
            return position < results.size();
        }

        @Override
        public Document next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return results.get(position++);
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
