package pt.tecnico.rrrd.server.utils;

import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

public class AuthorizationServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        if (!serverCall.getMethodDescriptor().getFullMethodName().equals("pt.tecnico.rrrd.contract.RemoteServer/Login")){
            String value = metadata.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));

            if (value == null) {
                serverCall.close(Status.UNAUTHENTICATED.withDescription("JWT Token is missing from Metadata"), metadata);
                return new ServerCall.Listener() {};
            }

            try {
                Jws<Claims> jws = Jwts.parserBuilder()
                        .setSigningKey(CryptographicOperations.getPublicKey("password", "asymmetric_keys"))
                        .build()
                        .parseClaimsJws(value);

                Context ctx = Context.current().withValue(Constants.CLIENT_ID_CONTEXT_KEY, jws.getBody().getSubject());

                return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
            } catch (Exception e) {
                System.out.println("Verification failed - Unauthenticated!");
                serverCall.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e), metadata);
                return new ServerCall.Listener() {};
            }

        } else {
            return Contexts.interceptCall(Context.current(), serverCall, metadata, serverCallHandler);
        }
    }
}
