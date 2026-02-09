package inf222.aop.measures;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MeasureAspect {

    private final Pattern pattern;
    private final Map<String, Double> toMeter = new HashMap<>(Map.of(
            "m", 1d,
            "ft", 0.3048d,
            "in", 0.0254d,
            "cm", 0.01d,
            "yd", 0.9144d));

    public MeasureAspect() {
        System.out.println(">>> MeasureAspect LOADED <<<");
        String units = String.join("|", toMeter.keySet());
        pattern = Pattern.compile(".*_(" + units + ")$");
    }


    // Advice for reading fields: convert to meters
    @Around("get(double inf222.aop.measures..*)")
    public Object convertToMeters(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();

        if (!(result instanceof Double)) return result;

        double value = (Double) result;
        String fieldName = pjp.getSignature().getName();

        Matcher matcher = pattern.matcher(fieldName);
        if (matcher.find()) {
            String unit = matcher.group(1);
            Double factor = toMeter.get(unit);
            if (factor != null) {
                return value * factor; // convert to meters
            }
        }

        return value;
    }


    // Advice for writing fields: convert back (exclude constructors)
    @Around("set(double inf222.aop.measures..*) && !cflow(execution(*.new(..)))")
    public void convertBackFromMeters(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        if (args.length == 1 && args[0] instanceof Double) {
            double valueInMeters = (Double) args[0];
            String fieldName = pjp.getSignature().getName();

            // Extract unit
            double valueInOriginalUnit = valueInMeters;
            Matcher matcher = pattern.matcher(fieldName);
            if (matcher.find()) {
                String unit = matcher.group(1);
                Double factor = toMeter.get(unit);
                if (factor != null) {
                    valueInOriginalUnit = valueInMeters / factor;
                }
            }

            // Proceed with assignment in original unit
            pjp.proceed(new Object[]{valueInOriginalUnit});
            return;
        }

        // fallback if not a double
        pjp.proceed();
    }


    // Advice for validating positive values (includes constructors)
    @Around("set(double inf222.aop.measures..*)")
    public void validatePositive(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        if (args.length == 1 && args[0] instanceof Double) {
            double newValue = (Double) args[0];

            if (newValue < 0) {
                throw new Error("Illegal modification");
            }
        }

        // proceed with assignment
        pjp.proceed();
    }
}