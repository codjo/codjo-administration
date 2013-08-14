package net.codjo.administration.server.operation.configuration;
/**
 *
 */
public class Query {
    private final String name;
    private final String query;
    private final long time;
    final int connectionId;


    public Query(String name, String query, long time, int connectionId) {
        this.name = name;
        this.query = query;
        this.time = time;
        this.connectionId = connectionId;
    }


    @Override
    public String toString() {
        return (name == null) ? query : name;
    }


    public String getQuery() {
        return query;
    }


    public long getTime() {
        return time;
    }


    public int getConnectionId() {
        return connectionId;
    }
}
