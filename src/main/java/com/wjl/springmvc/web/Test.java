package com.wjl.springmvc.web;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Test {

        public static void main(String[] args) throws Exception {
            //通过反射获取到方法
            Method declaredMethod = Test.class.getDeclaredMethod("findStr", int.class, Map.class);
            //获取返回值的类型，此处不是数组，请注意智商，返回值只能是一个
            Type genericReturnType = declaredMethod.getGenericReturnType();
            System.out.println(genericReturnType);
            //获取返回值的泛型参数
            if(genericReturnType instanceof ParameterizedType){
                Type[] actualTypeArguments = ((ParameterizedType)genericReturnType).getActualTypeArguments();
                for (Type type : actualTypeArguments) {
                    System.out.println(type);
                    if (type.toString().startsWith("class")) {
                        String string = "class" + " " + "java.lang.String";
                        String s = type.toString();
                        boolean equals = s.equals(string);
                        System.out.println(equals);
                    }
                }
            }
        }

        public static List<String> findStr(int id, Map<Integer, String> map){
            return null;
        }

}
