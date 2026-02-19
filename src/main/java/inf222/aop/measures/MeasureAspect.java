package inf222.aop.measures;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MeasureAspect {

    private final Pattern pattern;

    private final Map<String, Double> toMeter = Map.of(
            "cm", 0.01d,
            "ft", 0.3048d,
            "in", 0.0254d,
            "yd", 0.9144d,
            "m", 1d
    );

    public MeasureAspect() {
        // Longest first to avoid matching _m inside _cm
        pattern = Pattern.compile(".*_(cm|ft|in|yd|m)$");
    }

    // Convert to meters
    @Around("get(double inf222.aop.measures..*)")
    public Object convertToMeters(ProceedingJoinPoint pjp) throws Throwable {

        String fieldName = pjp.getSignature().getName();
        Matcher matcher = pattern.matcher(fieldName);

        if (!matcher.find()) {
            return pjp.proceed();
        }

        double value = (double) pjp.proceed();
        String unit = matcher.group(1);

        return value * toMeter.get(unit);
    }


    @Around("set(double inf222.aop.measures..*)")
    public void validateNegative(ProceedingJoinPoint pjp) throws Throwable {

        String fieldName = pjp.getSignature().getName();
        Matcher matcher = pattern.matcher(fieldName);

        if (!matcher.find()) {
            pjp.proceed();
            return;
        }

        Object[] args = pjp.getArgs();
        double value = (double) args[0];

        if (value < 0) {
            throw new Error("Illegal modification");
        }

        pjp.proceed();
    }

    @Around("set(double inf222.aop.measures..*) && !cflow(execution(*.new(..)))")
    public void convertBack(ProceedingJoinPoint pjp) throws Throwable {

        String fieldName = pjp.getSignature().getName();
        Matcher matcher = pattern.matcher(fieldName);

        if (!matcher.find()) {
            pjp.proceed();
            return;
        }

        Object[] args = pjp.getArgs();
        double valueInMeters = (double) args[0];

        String unit = matcher.group(1);
        double factor = toMeter.get(unit);

        double valueInOriginalUnit = valueInMeters / factor;

        pjp.proceed(new Object[]{valueInOriginalUnit});
    }
}