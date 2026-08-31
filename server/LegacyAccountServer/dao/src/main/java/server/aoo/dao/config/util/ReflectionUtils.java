package server.aoo.dao.config.util;
  
import java.lang.reflect.Field;  
  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
  
/** 
 * 方法类 
 * @author syh 
 * 
 */  
  
public class ReflectionUtils {  
    private static final System.Logger LOGGER = System.getLogger(ReflectionUtils.class.getName());
  
    /** 
     * 循环向上转型, 获     * @param object : 子类对象 
     * @param methodName : 父类中的方法名 
     * @param parameterTypes : 父类中的方法参数类型 
     * @return 父类中的方法对象 
     */  
      
    public static Method getDeclaredMethod(Object object, String methodName, Class<?> ... parameterTypes){  
        Method method = null ;  
          
        for(Class<?> clazz = object.getClass() ; clazz != Object.class ; clazz = clazz.getSuperclass()) {  
            try {  
                method = clazz.getDeclaredMethod(methodName, parameterTypes) ;  
                return method ;  
            } catch (Exception e) {  
                // exception-policy: superclass lookup continues when this class has no matching method.
                //这里甚么都不能抛出去。  
                //如果这里的异常打印或者往外抛，则就不会进入              
            }  
        }  
          
        return null;  
    }  
      
    /** 
     * 直接调用对象方法, 而忽略修饰符(private, protected, default) 
     * @param object : 子类对象 
     * @param methodName : 父类中的方法名 
     * @param parameterTypes : 父类中的方法参数类型 
     * @param parameters : 父类中的方法参数 
     * @return 父类中方法的执行结果 
     */  
      
    public static Object invokeMethod(Object object, String methodName, Class<?> [] parameterTypes,  
            Object [] parameters) {  
        //根据 对象、方法名和对应的方法参数 通过取 Method 对象  
        Method method = getDeclaredMethod(object, methodName, parameterTypes) ;  
          
        if (method == null) {
            return null;
        }
        //抑制Java对方法进行检查,主要是针对私有方法而言  
        method.setAccessible(true) ;  
          
            try {  
                //调用object 的 method 所代表的方法，其方法的参数是 parameters  
                return method.invoke(object, parameters) ;  
            } catch (IllegalArgumentException e) {  
                LOGGER.log(System.Logger.Level.ERROR, "Invalid reflection invocation arguments", e);
            } catch (IllegalAccessException e) {  
                LOGGER.log(System.Logger.Level.ERROR, "Unable to access reflected method", e);
            } catch (InvocationTargetException e) {  
                LOGGER.log(System.Logger.Level.ERROR, "Reflected method failed", e.getCause() == null ? e : e.getCause());
            }  
          
        return null;  
    }  
  
    /** 
     * 循环向上转型, 获     * @param object : 子类对象 
     * @param fieldName : 父类中     * @return 父类中     */  
      
    public static Field getDeclaredField(Object object, String fieldName){  
        Field field = null ;  
          
        Class<?> clazz = object.getClass() ;  
          
        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {  
            try {  
                field = clazz.getDeclaredField(fieldName) ;  
                return field ;  
            } catch (Exception e) {  
                // exception-policy: superclass lookup continues when this class has no matching field.
                //这里甚么都不能抛出去。  
                //如果这里的异常打印或者往外抛，则就不会进入                  
            }   
        }  
      
        return null;  
    }

    /**
     * 循环向上转型, 获     * @param object : 子类对象
     * @param fieldName : 父类中     * @return 父类中     */

    public static Field getDeclaredField(Class<?> clazz, String fieldName){
        Field field = null ;

        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName) ;
                return field ;
            } catch (Exception e) {
                // exception-policy: superclass lookup continues when this class has no matching field.
                //这里甚么都不能抛出去。
                //如果这里的异常打印或者往外抛，则就不会进入
            }
        }

        return null;
    }

    /** 
     * 直接设置对象属性值, 忽略 private/protected 修饰符, 也     * @param object : 子类对象 
     * @param fieldName : 父类中     * @param value : 将要设置的值 
     */  
      
    public static void setFieldValue(Object object, String fieldName, Object value){  
      
        //根据 对象和属性名通过取 Field对象  
        Field field = getDeclaredField(object, fieldName) ;  
          
        //抑制Java对其的检查  
        field.setAccessible(true) ;  
          
        try {  
            //将 object 中 field 所代表的值 设置为 value  
             field.set(object, value) ;  
        } catch (IllegalArgumentException e) {  
            LOGGER.log(System.Logger.Level.ERROR, "Invalid reflected field assignment", e);
        } catch (IllegalAccessException e) {  
            LOGGER.log(System.Logger.Level.ERROR, "Unable to access reflected field", e);
        }  
          
    }  
      
    /** 
     * 直接读的属性值, 忽略 private/protected 修饰符, 也     * @param object : 子类对象 
     * @param fieldName : 父类中     * @return : 父类中     */  
      
    public static Object getFieldValue(Object object, String fieldName){  
          
        //根据 对象和属性名通过取 Field对象  
        Field field = getDeclaredField(object, fieldName) ;  
          
        //抑制Java对其的检查  
        field.setAccessible(true) ;  
          
        try {  
            //获的属性值  
            return field.get(object) ;  
              
        } catch(Exception e) {  
            LOGGER.log(System.Logger.Level.ERROR, "Reflection operation failed", e);
        }  
          
        return null;  
    }  
} 
