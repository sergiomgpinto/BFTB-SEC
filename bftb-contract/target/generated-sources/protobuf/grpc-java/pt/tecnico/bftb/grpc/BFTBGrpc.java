package pt.tecnico.bftb.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.28.0)",
    comments = "Source: bftb.proto")
public final class BFTBGrpc {

  private BFTBGrpc() {}

  public static final String SERVICE_NAME = "pt.tecnico.bftb.grpc.BFTB";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.NonceRequest,
      pt.tecnico.bftb.grpc.Bftb.NonceResponse> getGetNonceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getNonce",
      requestType = pt.tecnico.bftb.grpc.Bftb.NonceRequest.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.NonceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.NonceRequest,
      pt.tecnico.bftb.grpc.Bftb.NonceResponse> getGetNonceMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.NonceRequest, pt.tecnico.bftb.grpc.Bftb.NonceResponse> getGetNonceMethod;
    if ((getGetNonceMethod = BFTBGrpc.getGetNonceMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getGetNonceMethod = BFTBGrpc.getGetNonceMethod) == null) {
          BFTBGrpc.getGetNonceMethod = getGetNonceMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.NonceRequest, pt.tecnico.bftb.grpc.Bftb.NonceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getNonce"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.NonceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.NonceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("getNonce"))
              .build();
        }
      }
    }
    return getGetNonceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getOpenAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "openAccount",
      requestType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getOpenAccountMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getOpenAccountMethod;
    if ((getOpenAccountMethod = BFTBGrpc.getOpenAccountMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getOpenAccountMethod = BFTBGrpc.getOpenAccountMethod) == null) {
          BFTBGrpc.getOpenAccountMethod = getOpenAccountMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "openAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("openAccount"))
              .build();
        }
      }
    }
    return getOpenAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getSendAmountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sendAmount",
      requestType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getSendAmountMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getSendAmountMethod;
    if ((getSendAmountMethod = BFTBGrpc.getSendAmountMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getSendAmountMethod = BFTBGrpc.getSendAmountMethod) == null) {
          BFTBGrpc.getSendAmountMethod = getSendAmountMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sendAmount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("sendAmount"))
              .build();
        }
      }
    }
    return getSendAmountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getCheckAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "checkAccount",
      requestType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getCheckAccountMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getCheckAccountMethod;
    if ((getCheckAccountMethod = BFTBGrpc.getCheckAccountMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getCheckAccountMethod = BFTBGrpc.getCheckAccountMethod) == null) {
          BFTBGrpc.getCheckAccountMethod = getCheckAccountMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "checkAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("checkAccount"))
              .build();
        }
      }
    }
    return getCheckAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getAuditMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "audit",
      requestType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getAuditMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getAuditMethod;
    if ((getAuditMethod = BFTBGrpc.getAuditMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getAuditMethod = BFTBGrpc.getAuditMethod) == null) {
          BFTBGrpc.getAuditMethod = getAuditMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "audit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("audit"))
              .build();
        }
      }
    }
    return getAuditMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getReceiveAmountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "receiveAmount",
      requestType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
      pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getReceiveAmountMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> getReceiveAmountMethod;
    if ((getReceiveAmountMethod = BFTBGrpc.getReceiveAmountMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getReceiveAmountMethod = BFTBGrpc.getReceiveAmountMethod) == null) {
          BFTBGrpc.getReceiveAmountMethod = getReceiveAmountMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck, pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "receiveAmount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.EncryptedStruck.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("receiveAmount"))
              .build();
        }
      }
    }
    return getReceiveAmountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest,
      pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse> getSearchKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "searchKeys",
      requestType = pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest.class,
      responseType = pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest,
      pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse> getSearchKeysMethod() {
    io.grpc.MethodDescriptor<pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest, pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse> getSearchKeysMethod;
    if ((getSearchKeysMethod = BFTBGrpc.getSearchKeysMethod) == null) {
      synchronized (BFTBGrpc.class) {
        if ((getSearchKeysMethod = BFTBGrpc.getSearchKeysMethod) == null) {
          BFTBGrpc.getSearchKeysMethod = getSearchKeysMethod =
              io.grpc.MethodDescriptor.<pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest, pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "searchKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BFTBMethodDescriptorSupplier("searchKeys"))
              .build();
        }
      }
    }
    return getSearchKeysMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BFTBStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BFTBStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BFTBStub>() {
        @java.lang.Override
        public BFTBStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BFTBStub(channel, callOptions);
        }
      };
    return BFTBStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BFTBBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BFTBBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BFTBBlockingStub>() {
        @java.lang.Override
        public BFTBBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BFTBBlockingStub(channel, callOptions);
        }
      };
    return BFTBBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static BFTBFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BFTBFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BFTBFutureStub>() {
        @java.lang.Override
        public BFTBFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BFTBFutureStub(channel, callOptions);
        }
      };
    return BFTBFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class BFTBImplBase implements io.grpc.BindableService {

    /**
     */
    public void getNonce(pt.tecnico.bftb.grpc.Bftb.NonceRequest request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.NonceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNonceMethod(), responseObserver);
    }

    /**
     */
    public void openAccount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnimplementedUnaryCall(getOpenAccountMethod(), responseObserver);
    }

    /**
     */
    public void sendAmount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnimplementedUnaryCall(getSendAmountMethod(), responseObserver);
    }

    /**
     */
    public void checkAccount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnimplementedUnaryCall(getCheckAccountMethod(), responseObserver);
    }

    /**
     */
    public void audit(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnimplementedUnaryCall(getAuditMethod(), responseObserver);
    }

    /**
     */
    public void receiveAmount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnimplementedUnaryCall(getReceiveAmountMethod(), responseObserver);
    }

    /**
     */
    public void searchKeys(pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSearchKeysMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetNonceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.NonceRequest,
                pt.tecnico.bftb.grpc.Bftb.NonceResponse>(
                  this, METHODID_GET_NONCE)))
          .addMethod(
            getOpenAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>(
                  this, METHODID_OPEN_ACCOUNT)))
          .addMethod(
            getSendAmountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>(
                  this, METHODID_SEND_AMOUNT)))
          .addMethod(
            getCheckAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>(
                  this, METHODID_CHECK_ACCOUNT)))
          .addMethod(
            getAuditMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>(
                  this, METHODID_AUDIT)))
          .addMethod(
            getReceiveAmountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck,
                pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>(
                  this, METHODID_RECEIVE_AMOUNT)))
          .addMethod(
            getSearchKeysMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest,
                pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse>(
                  this, METHODID_SEARCH_KEYS)))
          .build();
    }
  }

  /**
   */
  public static final class BFTBStub extends io.grpc.stub.AbstractAsyncStub<BFTBStub> {
    private BFTBStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BFTBStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BFTBStub(channel, callOptions);
    }

    /**
     */
    public void getNonce(pt.tecnico.bftb.grpc.Bftb.NonceRequest request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.NonceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNonceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void openAccount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getOpenAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendAmount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendAmountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void checkAccount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCheckAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void audit(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAuditMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void receiveAmount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReceiveAmountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void searchKeys(pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest request,
        io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSearchKeysMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class BFTBBlockingStub extends io.grpc.stub.AbstractBlockingStub<BFTBBlockingStub> {
    private BFTBBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BFTBBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BFTBBlockingStub(channel, callOptions);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.NonceResponse getNonce(pt.tecnico.bftb.grpc.Bftb.NonceRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetNonceMethod(), getCallOptions(), request);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.EncryptedStruck openAccount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return blockingUnaryCall(
          getChannel(), getOpenAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.EncryptedStruck sendAmount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return blockingUnaryCall(
          getChannel(), getSendAmountMethod(), getCallOptions(), request);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.EncryptedStruck checkAccount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return blockingUnaryCall(
          getChannel(), getCheckAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.EncryptedStruck audit(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return blockingUnaryCall(
          getChannel(), getAuditMethod(), getCallOptions(), request);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.EncryptedStruck receiveAmount(pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return blockingUnaryCall(
          getChannel(), getReceiveAmountMethod(), getCallOptions(), request);
    }

    /**
     */
    public pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse searchKeys(pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest request) {
      return blockingUnaryCall(
          getChannel(), getSearchKeysMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class BFTBFutureStub extends io.grpc.stub.AbstractFutureStub<BFTBFutureStub> {
    private BFTBFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BFTBFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BFTBFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.NonceResponse> getNonce(
        pt.tecnico.bftb.grpc.Bftb.NonceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNonceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> openAccount(
        pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return futureUnaryCall(
          getChannel().newCall(getOpenAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> sendAmount(
        pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return futureUnaryCall(
          getChannel().newCall(getSendAmountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> checkAccount(
        pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return futureUnaryCall(
          getChannel().newCall(getCheckAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> audit(
        pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return futureUnaryCall(
          getChannel().newCall(getAuditMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck> receiveAmount(
        pt.tecnico.bftb.grpc.Bftb.EncryptedStruck request) {
      return futureUnaryCall(
          getChannel().newCall(getReceiveAmountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse> searchKeys(
        pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSearchKeysMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_NONCE = 0;
  private static final int METHODID_OPEN_ACCOUNT = 1;
  private static final int METHODID_SEND_AMOUNT = 2;
  private static final int METHODID_CHECK_ACCOUNT = 3;
  private static final int METHODID_AUDIT = 4;
  private static final int METHODID_RECEIVE_AMOUNT = 5;
  private static final int METHODID_SEARCH_KEYS = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BFTBImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BFTBImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_NONCE:
          serviceImpl.getNonce((pt.tecnico.bftb.grpc.Bftb.NonceRequest) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.NonceResponse>) responseObserver);
          break;
        case METHODID_OPEN_ACCOUNT:
          serviceImpl.openAccount((pt.tecnico.bftb.grpc.Bftb.EncryptedStruck) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>) responseObserver);
          break;
        case METHODID_SEND_AMOUNT:
          serviceImpl.sendAmount((pt.tecnico.bftb.grpc.Bftb.EncryptedStruck) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>) responseObserver);
          break;
        case METHODID_CHECK_ACCOUNT:
          serviceImpl.checkAccount((pt.tecnico.bftb.grpc.Bftb.EncryptedStruck) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>) responseObserver);
          break;
        case METHODID_AUDIT:
          serviceImpl.audit((pt.tecnico.bftb.grpc.Bftb.EncryptedStruck) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>) responseObserver);
          break;
        case METHODID_RECEIVE_AMOUNT:
          serviceImpl.receiveAmount((pt.tecnico.bftb.grpc.Bftb.EncryptedStruck) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.EncryptedStruck>) responseObserver);
          break;
        case METHODID_SEARCH_KEYS:
          serviceImpl.searchKeys((pt.tecnico.bftb.grpc.Bftb.SearchKeysRequest) request,
              (io.grpc.stub.StreamObserver<pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class BFTBBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BFTBBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pt.tecnico.bftb.grpc.Bftb.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BFTB");
    }
  }

  private static final class BFTBFileDescriptorSupplier
      extends BFTBBaseDescriptorSupplier {
    BFTBFileDescriptorSupplier() {}
  }

  private static final class BFTBMethodDescriptorSupplier
      extends BFTBBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BFTBMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (BFTBGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BFTBFileDescriptorSupplier())
              .addMethod(getGetNonceMethod())
              .addMethod(getOpenAccountMethod())
              .addMethod(getSendAmountMethod())
              .addMethod(getCheckAccountMethod())
              .addMethod(getAuditMethod())
              .addMethod(getReceiveAmountMethod())
              .addMethod(getSearchKeysMethod())
              .build();
        }
      }
    }
    return result;
  }
}
