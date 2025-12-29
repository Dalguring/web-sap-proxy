package com.prototype.proxy.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class RequestContext {

    private String requestId;
    private String interfaceId;
}
