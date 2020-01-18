package com.wjl.springmvc.servlet;

import com.wjl.springmvc.annatation.MyController;
import com.wjl.springmvc.annatation.MyRequestMapping;
import com.wjl.springmvc.util.ClassUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.创建一个前端控制器（中央控制器）DispatcherServlet 拦截所有请求（SpringMVC基于Servlet实现）
 * 2.初始化操作,实际上就是重写Servlet的init方法。
 * ##2.1 将扫包范围内的所有类，注入到SpringIOC容器(当然他得带有@Controller注解才会被注入）
 * ##2.2 将url映射和方法关联
 * ###2.2.1 判断类上是否存在注解，使用Java反射机制循环遍历方法，判断方法上是否存在注解，进行封装url和方法对应。
 * 3.处理请求 重写Get或者是Post方法（实际上doService方法也可以但是这里偷懒了主要是跑一下流程）
 * ##3.1 获取url请求，从urlBeans集合获取实例对象，获取成功实例对象后，调用urlMethods集合获取方法名称，使用反射机制执行。
 * @data 2018/3/18 14:57
 * @by wjl
*/
public class MyDispatcherServlet extends HttpServlet {
    /**
     *  定义三个容器
     *  第一个容器实际上就是我们的SpringIOC容器,
     *  在这里对ConcurrentHashMap初始化
     *  实际上这样是不对的，有可能会导致内存泄漏的问题，但是在这里我们暂时不做处理
      * @throws
      * @data 2018/3/18 15:01
      * @by wjl
    */
    private ConcurrentHashMap<String,Object> springMvcBeans = new ConcurrentHashMap<String, Object>();
    //Controller类的容器
    private ConcurrentHashMap<String,Object> urlBeans = new ConcurrentHashMap<String, Object>();
    /**
     * Controller类中带有RequestMapping注解的方法的容器
     * 实际上我们也可以吧这个容器直接放在类的容器中，虽然这样可以减少部分代码量，
     * 但是这样会导致查询的时候变得很臃肿，降低查询的效率，所以我在这里对两个容器进行了拆分
     * 在这里我们存入的是方法的名称，实际上我们应该存入的是ConcurrentHashMap<String,Object>,k 方法名,v 方法的参数类型
     * 这样我们才能够将获取到的参数传递给需要执行的控制方法中,但是我暂时还未进行这一步操作
      * @throws
      * @data 2018/3/18 15:03
      * @by wjl
     *
     * @data 2018/5/23 10:27
     * 如果我们要带参数的话,jdk1.8之前反射是无法获取到参数名称的
     * 在SpringMVC中使用的ASM技术获取方法参数的名称(不会）
     *最好不再使用map而是封装到一个类中存储这些信息
    */
    private ConcurrentHashMap<String,String> methodsBeans = new ConcurrentHashMap<String, String>();
    @Override
    public void init() throws ServletException {
        //在这里我们初始化SpringMVC容器
        //在原生的SpringMVC中我们是通过init方法扫描获取Spring.xml配置文件方式来获取扫包路径
        //由于这不是核心内容所以这里作为参数传递进init方法中
        //1.扫包并初始化容器，将需要扫描的包传入进入
        String packageName = "com.wjl.springmvc";
        try {
        initBeans(packageName);
            System.out.println(springMvcBeans.get("userController"));
        //下一步handerMapping配置url映射到方法的控制
        handerMapping();
        }catch (Exception e){

        }
    }

    //映射控制处理器
    private void handerMapping() {
        //初始化完成容器之后
        //遍历map集合在ioc的时候已经提到过
        for (Map.Entry<String, Object> objectEntry : springMvcBeans.entrySet()) {
            Object obj = objectEntry.getValue();
            Class<?> clazz = obj.getClass();
            MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
            String baseUrl = "";
            if (myRequestMapping != null){
                baseUrl = myRequestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            //下面是对方法的处理
            for (Method method : methods) {
                MyRequestMapping methodRequestMapping = method.getAnnotation(MyRequestMapping.class);
                if (methodRequestMapping != null) {
                    String methodUrl = baseUrl + methodRequestMapping.value();
                    //springmvn 容器对象 keya:请求地址，value类
                    urlBeans.put(methodUrl,obj);
                    //这里只存了方法名的原因(暂时这么操作）
                    //之后反射调用需要类+方法名，如果直接存入方法不知能否运行
                    methodsBeans.put(methodUrl,method.getName());
                }
            }
        }
    }

    private void initBeans(String packageName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        //由于重点是SpringMVC所以并没有去研究如何扫包
        //这个扫包的工具类只有一部分是我自己写的
        //其余是转自大神博客
        List<Class<?>> classList = ClassUtil.getClassList(packageName, true);
        for (Class<?> clazz : classList) {
            //判断是否带有注解
            isAnnatation(clazz);
        }
    }

    private void isAnnatation(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        MyController myController = clazz.getAnnotation(MyController.class);
        if (myController != null){
            //将对象实例放入到容器中
            springMvcBeans.put(ClassUtil.toLowerCase(clazz),ClassUtil.newInstance(clazz));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doPost(req,resp);
    }

    /**
     *
     * 在这个方法里我们来根据请求路径获取要执行的类和方法
     * 并将方法执行取到方法的返回值
     * 1.获取到请求的地址
     * 2.取出需要执行的类
     * 3.取出需要执行的方法
     * 4.执行方法
     * 5.返回执行的结果，也就是获取到方法的返回值
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @by wjl
     * @Data 2018/3/18 16:34
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        if (StringUtils.isEmpty(requestURI)){
            return;
        }
        Object object = urlBeans.get(requestURI);
        //如果这个类不存在的话
        //实际上就是地址出现了错误=>404
        if (object == null) {
            resp.getWriter().println("Not fount Resource does not exist 404 ");
            return;
        }
        String methodName = methodsBeans.get(requestURI);
        if (StringUtils.isEmpty(methodName)) {
            resp.getWriter().println("404 Not fount Resource does not exist");
        }
        //这里我们执行这个方法，由于前面未做参数的获取所以这里不传参数
        Map<String,Object> result = methodIn(object,methodName);
        if ((Boolean)result.get("view")){
            resolutionView((String)result.get("methodResult"),req,resp);
        }else {
            resp.getWriter().write(result.get("methodResult").toString());
        }
    }

    private void resolutionView(String methodResult, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //这里的头尾因为我们没有专门去编写视图解析器的类
        //也没有去进行xml的配置扫描,所以就定义在方法中模拟一下情况
        String prefix = "/web/";
        String suffix = ".html";
        //直接转发到该页面,此处有个Bug转发过去req之后又会被拦截住导致404
        req.getRequestDispatcher(prefix+methodResult+suffix).forward(req,resp);
    }

    private Map<String,Object> methodIn(Object object, String methodName) {
        Map<String,Object> result = new HashMap<String, Object>();
        Class<?> clazz = object.getClass();
        try {
            Method method = clazz.getMethod(methodName);
            Type returnType = method.getReturnType();
            String s = returnType.toString();
            //这一段实在想不出如何动态的获取返回值的类型并且返回出去
            // 就作假模拟一下返回的是String的时候我就判断你是视图进行视图解析
            if (s.startsWith("class")) {
                String string = "class" + " " + "java.lang.String";
                if (s.equals(string)){
                    result.put("view",true);
                }else {
                    result.put("view",false);
                }
            }
            Object methodResult = method.invoke(object);
            result.put("methodResult",methodResult);
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
