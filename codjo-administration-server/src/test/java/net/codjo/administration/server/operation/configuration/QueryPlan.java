package net.codjo.administration.server.operation.configuration;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.TreeSet;
import net.codjo.sql.spy.stats.Statistics;
/**
 *
 */
public class QueryPlan {
    private final Query[] queries;


    public QueryPlan(Query... queries) {
        this.queries = queries;
    }


    public Set<Integer> getConnectionIds() {
        Set<Integer> result = new TreeSet<Integer>();
        for (Query query : queries) {
            result.add(query.connectionId);
        }
        return result;
    }


    public Statistics computeStats(String query1, Integer connectionId)
          throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Statistics result = new Statistics();

        for (int i = 0; i < queries.length; i++) {
            Query query = queries[i];
            if (query.getQuery().equals(query1) && ((connectionId == null) || (connectionId.intValue()
                                                                               == query.connectionId))) {
                result.inc();
                result.addTime(queries[i].getTime());
            }
        }

        return result;
    }


    public Query[] getQueries() {
        return queries;
    }
}
