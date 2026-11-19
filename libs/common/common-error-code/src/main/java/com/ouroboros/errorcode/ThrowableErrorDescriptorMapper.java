package com.ouroboros.errorcode;

public interface ThrowableErrorDescriptorMapper {

  boolean supports(Throwable throwable);

  ResolvedErrorDescriptor map(Throwable throwable);
}
