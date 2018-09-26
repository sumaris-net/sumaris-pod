package net.sumaris.server.http.graphql;

public interface GraphQLPaths {
    String BASE_PATH = "/graphql";

    String SUBSCRIPTION_PATH = BASE_PATH + "/websocket";
}
