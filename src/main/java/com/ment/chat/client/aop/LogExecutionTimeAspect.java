package com.ment.chat.client.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogExecutionTimeAspect {

    @Around("@annotation(LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startExecutionTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long stopExecutionTime = System.currentTimeMillis();
        log.info("Execution time of {}: {} ms", joinPoint.getSignature(), (stopExecutionTime - startExecutionTime));
        return result;
    }
}
