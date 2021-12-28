package com.fengyue95.logspringbootstarter.placeholderlog.aspect;

import java.lang.reflect.Method;
import java.util.Objects;

import com.fengyue95.logspringbootstarter.methodparamlog.cache.LoggerCache;
import com.fengyue95.logspringbootstarter.placeholderlog.annotation.PlaceholderLog;
import com.fengyue95.logspringbootstarter.util.SpringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * @author fengyue 支持识别指定类以及方法入参对象属性获取，打印日志
 * @date 2021/11/4
 */
@Aspect
@Component
public class PlaceholderLogAspect {

    private static final Logger                             logger     = LoggerFactory.getLogger(PlaceholderLogAspect.class);

    private final ExpressionParser                          parser     = new SpelExpressionParser();
    private final LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Pointcut("@annotation(com.fengyue95.logspringbootstarter.placeholderlog.annotation.PlaceholderLog)")
    public void log() {
    }

    @Around(value = "log()")
    public void logBefore(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 获取注解所在的类名
            String className = joinPoint.getTarget().getClass().getName();
            Logger classlog = LoggerCache.getLogger(className);

            // 获取注解所在的方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 获取注解，解析注解content参数
            EvaluationContext context = new StandardEvaluationContext();
            PlaceholderLog annotation = method.getAnnotation(PlaceholderLog.class);
            String content = annotation.content();
            Object[] args = joinPoint.getArgs();
            Class clazz = annotation.currentUser();
            if (args.length != 0) {
                // 解析入参
                bindMethodParam(method, args, context);
            }
            if (Objects.nonNull(clazz)) {
                // 解析上下文对象
                bindCurrentUser(clazz, context);
            }
            // 根据spel表达式获取值
            Expression expression = parser.parseExpression(content, new TemplateParserContext());
            Object key = expression.getValue(context);
            classlog.info("className:{},{}", className, key);
        } catch (Exception e) {
            // 注解的异常不影响方法的执行
            logger.warn("PlaceholderLogAspect execute error:", e);
        }
        joinPoint.proceed();
    }

    /**
     * 将方法的参数名和参数值绑定 方法参数解析
     *
     * @param method 方法，根据方法获取参数名
     * @param args 方法的参数值
     * @return
     */
    private void bindMethodParam(Method method, Object[] args, EvaluationContext context) {
        // 获取方法的参数名
        String[] params = discoverer.getParameterNames(method);
        if (params.length == 0) {
            return;/**/
        }
        // 将参数名与参数值对应起来
        for (int len = 0; len < args.length; len++) {
            context.setVariable(params[len], args[len]);
        }
    }

    /**
     * 将方法的参数名和参数值绑定 指定类解析
     *
     * @param clazz 方法，根据方法获取参数名
     * @return
     */
    private void bindCurrentUser(Class clazz, EvaluationContext context) {
        if (Objects.isNull(clazz)) {
            return;
        }
        // 获取传入的上下文对象
        Object bean = SpringUtil.getBean(clazz);
        String[] split = clazz.getName().split("\\.");
        String application = split[split.length - 1];
        String name = application.substring(0, 1).toLowerCase() + application.substring(1);
        // 将参数名与参数值对应起来
        context.setVariable(name, bean);
    }
}
