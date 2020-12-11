package pt.tecnico.rrrd.server.utils;

import io.grpc.Context;

public class Constants {
    public static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");
}
