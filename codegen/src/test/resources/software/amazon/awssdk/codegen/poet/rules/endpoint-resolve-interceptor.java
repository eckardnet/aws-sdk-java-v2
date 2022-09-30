package software.amazon.awssdk.services.query.rules.internal;

import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.rules.AwsEndpointProviderUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;
import software.amazon.awssdk.services.query.rules.QueryClientContextParams;
import software.amazon.awssdk.services.query.rules.QueryEndpointParams;
import software.amazon.awssdk.services.query.rules.QueryEndpointProvider;
import software.amazon.awssdk.utils.AttributeMap;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class QueryResolveEndpointInterceptor implements ExecutionInterceptor {
    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        if (AwsEndpointProviderUtils.endpointIsDiscovered(executionAttributes)) {
            return context.request();
        }
        QueryEndpointProvider provider = (QueryEndpointProvider) executionAttributes
            .getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        Endpoint result = provider.resolveEndpoint(ruleParams(context, executionAttributes));
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, result);
        return context.request();
    }

    private static QueryEndpointParams ruleParams(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
        setStaticContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME));
        setContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME), context.request());
        setClientContextParams(builder, executionAttributes);
        builder.region(AwsEndpointProviderUtils.regionBuiltIn(executionAttributes));
        builder.useDualStackEndpoint(AwsEndpointProviderUtils.dualStackEnabledBuiltIn(executionAttributes));
        builder.useFipsEndpoint(AwsEndpointProviderUtils.fipsEnabledBuiltIn(executionAttributes));
        return builder.build();
    }

    private static void setContextParams(QueryEndpointParams.Builder params, String operationName, SdkRequest request) {
        switch (operationName) {
            case "OperationWithContextParam":
                setContextParams(params, (OperationWithContextParamRequest) request);
                break;
            default:
                break;
        }
    }

    private static void setContextParams(QueryEndpointParams.Builder params, OperationWithContextParamRequest request) {
        params.operationContextParam(request.stringMember());
    }

    private static void setStaticContextParams(QueryEndpointParams.Builder params, String operationName) {
        switch (operationName) {
            case "OperationWithStaticContextParams":
                operationWithStaticContextParamsStaticContextParams(params);
                break;
            default:
                break;
        }
    }

    private static void operationWithStaticContextParamsStaticContextParams(QueryEndpointParams.Builder params) {
        params.staticStringParam("hello");
    }

    private static void setClientContextParams(QueryEndpointParams.Builder params, ExecutionAttributes executionAttributes) {
        AttributeMap clientContextParams = executionAttributes.getAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS);
        Optional.ofNullable(clientContextParams.get(QueryClientContextParams.BOOLEAN_CONTEXT_PARAM)).ifPresent(
            params::booleanContextParam);
        Optional.ofNullable(clientContextParams.get(QueryClientContextParams.STRING_CONTEXT_PARAM)).ifPresent(
            params::stringContextParam);
    }
}
