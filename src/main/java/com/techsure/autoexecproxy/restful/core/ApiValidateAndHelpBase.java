package com.techsure.autoexecproxy.restful.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.exception.ParamIrregularException;
import com.techsure.autoexecproxy.exception.ParamNotExistsException;
import com.techsure.autoexecproxy.exception.ParamValueTooLongException;
import com.techsure.autoexecproxy.exception.ParamValueTooShortException;
import com.techsure.autoexecproxy.param.validate.core.ParamValidatorFactory;
import com.techsure.autoexecproxy.restful.annotation.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public class ApiValidateAndHelpBase {
    private static final Logger logger = LoggerFactory.getLogger(ApiValidateAndHelpBase.class);



    private static void escapeXss(JSONObject paramObj, String key) {
        Object value = paramObj.get(key);
        if (value instanceof String) {
            try {
                JSONObject valObj = JSONObject.parseObject(value.toString());
                escapeXss(valObj);
                paramObj.replace(key, valObj.toJSONString());
            } catch (Exception ex) {
                try {
                    JSONArray valList = JSONArray.parseArray(value.toString());
                    encodeHtml(valList);
                    paramObj.replace(key, valList.toJSONString());
                } catch (Exception e) {
                    paramObj.replace(key, escapeXss(value.toString()));
                }
            }
        } else if (value instanceof JSONObject) {
            escapeXss((JSONObject) value);
            paramObj.replace(key, value);
        } else if (value instanceof JSONArray) {
            encodeHtml((JSONArray) value);
            paramObj.replace(key, value);
        }
    }

	/*
	private static Pattern scriptPattern = Pattern.compile("<script(.*?)</script>", Pattern.CASE_INSENSITIVE);
	private static Pattern javascriptPattern = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
	private static Pattern evalPattern = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOLL);
	*/

    private static String escapeXss(String str) {
        if (StringUtils.isNotBlank(str)) {
			/*
			 处理js xss注入
			 str = scriptPattern.matcher(str).replaceAll("");
			 str = javascriptPattern.matcher(str).replaceAll("");
			 str = evalPattern.matcher(str).replaceAll("");
			*/
            str = str.replace("&", "&amp;");
            str = str.replace("<", "&lt;");
            str = str.replace(">", "&gt;");
            str = str.replace("'", "&#39;");
            str = str.replace("\"", "&quot;");

            return str;
        }
        return "";
    }


    private static void escapeXss(JSONObject j) {
        Set<String> set = j.keySet();
        for (String s : set) {
            String newKey = escapeXss(s);
            if (!newKey.equals(s)) {
                Object value = j.get(s);
                j.remove(s);
                j.replace(newKey, value);
            }
            if (j.get(newKey) instanceof JSONObject) {
                escapeXss(j.getJSONObject(newKey));
            } else if (j.get(newKey) instanceof JSONArray) {
                encodeHtml(j.getJSONArray(newKey));
            } else if (j.get(newKey) instanceof String) {
                j.replace(newKey, escapeXss(j.getString(newKey)));
            }
        }
    }

    private static void encodeHtml(JSONArray j) {
        for (int i = 0; i < j.size(); i++) {
            if (j.get(i) instanceof JSONObject) {
                escapeXss(j.getJSONObject(i));
            } else if (j.get(i) instanceof JSONArray) {
                encodeHtml(j.getJSONArray(i));
            } else if (j.get(i) instanceof String) {
                j.set(i, escapeXss(j.getString(i)));
            }
        }
    }

    protected void validApi(Class<?> apiClass, JSONObject paramObj, Class<?>... classes) throws NoSuchMethodException, SecurityException {
        // 获取目标类
        boolean isAuth = false;
        if (apiClass != null) {

            // 判断参数是否合法
            Method method = apiClass.getMethod("myDoService", classes);
            Input input = method.getAnnotation(Input.class);
            if (input != null) {
                Param[] params = input.value();
                if (params.length > 0) {
                    for (Param p : params) {
                        if (p.type().equals(ApiParamType.NOAUTH)) {
                            continue;
                        }
                        // xss过滤
                        if (p.xss() && paramObj.containsKey(p.name())) {
                            escapeXss(paramObj, p.name());
                        }

                        Object paramValue = null;

                        if (paramObj.containsKey(p.name())) {
                            // 参数类型校验
                            paramValue = paramObj.get(p.name());
                            // 如果值为null，则remove（前端约定不使用的接口参数会传null过来，故去掉）
                            if (paramValue == null) {
                                paramObj.remove(p.name());
                            }
                        }
                        // 前后去空格
                        if (p.type().equals(ApiParamType.STRING)) {
                            if (paramValue != null) {
                                paramObj.replace(p.name(), paramValue.toString().trim());
                            }
                        }
                        // 判断是否必填
                        if (p.isRequired() && !paramObj.containsKey(p.name())) {
                            throw new ParamNotExistsException("参数：“" + p.name() + "”不能为空");
                        }

                        // 判断最大长度
                        if (p.maxLength() > 0 && paramValue instanceof String) {
                            if (paramValue.toString().trim().length() > p.maxLength()) {
                                throw new ParamValueTooLongException(p.name(), paramValue.toString().length(), p.maxLength());
                            }
                        }
                        // 判断最小长度
                        if (p.minLength() > 0 && paramValue instanceof String) {
                            if (paramValue.toString().trim().length() < p.minLength()) {
                                throw new ParamValueTooShortException(p.name(), paramValue.toString().length(), p.minLength());
                            }
                        }
                        if (paramValue != null && !ParamValidatorFactory.getAuthInstance(p.type()).validate(paramValue, p.rule())) {
                            throw new ParamIrregularException("参数“" + p.desc() + "（" +p.name() + "）" + "”不符合格式要求");
                        }
                    }
                }
            }
        }
    }

    /*
     * @Description: 递归抽取字段信息
     * @Author: chenqiwei
     * @Date: 2021/1/8 2:14 下午
     * @Params: [field, paramList, loop]
     * @Returns: void
     **/
    private void drawFieldMessageRecursive(Field field, JSONArray paramList, boolean loop) {
        Annotation[] annotations = field.getDeclaredAnnotations();
        if (annotations.length > 0) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(EntityField.class)) {
                    EntityField entityField = (EntityField) annotation;
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("name", field.getName());
                    paramObj.put("type", entityField.type().getValue() + "[" + entityField.type().getText() + "]");
                    paramObj.put("description", entityField.name());

                    if (loop && field.getType().isAssignableFrom(List.class)) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) genericType;
                            Type actualType = parameterizedType.getActualTypeArguments()[0];
                            Class<?> integerClass = (Class<?>) actualType;
                            JSONArray subParamList = new JSONArray();
                            for (Field subField : integerClass.getDeclaredFields()) {
                                drawFieldMessageRecursive(subField, subParamList, false);
                            }
                            paramObj.put("children", subParamList);
                        }
                    }
                    paramList.add(paramObj);
                }
            }
        }
    }

    protected final JSONObject getApiComponentHelp(Class<?>... arg) {
        JSONObject jsonObj = new JSONObject();
        JSONArray inputList = new JSONArray();
        JSONArray outputList = new JSONArray();
        try {
            Method method = this.getClass().getDeclaredMethod("myDoService", arg);
            if (method.isAnnotationPresent(Input.class) || method.isAnnotationPresent(Output.class) || method.isAnnotationPresent(Description.class)) {
                for (Annotation anno : method.getDeclaredAnnotations()) {
                    if (anno.annotationType().equals(Input.class)) {
                        Input input = (Input) anno;
                        Param[] params = input.value();
                        if (params.length > 0) {
                            for (Param p : params) {

                                JSONObject paramObj = new JSONObject();
                                paramObj.put("name", p.name());
                                paramObj.put("type", p.type().getValue() + "[" + p.type().getText() + "]");
                                paramObj.put("isRequired", p.isRequired());
                                String description = p.desc();
                                if (StringUtils.isNotBlank(p.rule())) {
                                    description = description + "，合法输入：" + p.rule();
                                }
                                paramObj.put("description", description);
                                inputList.add(paramObj);
                                // }
                            }
                        }
                    } else if (anno.annotationType().equals(Output.class)) {
                        Output output = (Output) anno;
                        Param[] params = output.value();
                        if (params.length > 0) {
                            for (Param p : params) {
                                if (!p.explode().getName().equals(NotDefined.class.getName())) {
                                    //String paramNamePrefix = p.name();
                                    if (!p.explode().isArray()) {
                                        //paramNamePrefix = StringUtils.isBlank(paramNamePrefix) || "Return".equals(paramNamePrefix) ? "" : paramNamePrefix + ".";
                                        for (Field field : p.explode().getDeclaredFields()) {
                                            drawFieldMessageRecursive(field, outputList, true);
                                        }
                                    } else {
                                        JSONObject paramObj = new JSONObject();
                                        paramObj.put("name", p.name());
                                        paramObj.put("type", ApiParamType.JSONARRAY.getValue() + "[" + ApiParamType.JSONARRAY.getText() + "]");
                                        paramObj.put("description", p.desc());
                                        JSONArray elementObjList = new JSONArray();
                                        for (Field field : p.explode().getComponentType().getDeclaredFields()) {
                                            drawFieldMessageRecursive(field, elementObjList, true);
                                        }
                                        if (elementObjList.size() > 0) {
                                            paramObj.put("children", elementObjList);
                                        }

                                        outputList.add(paramObj);
                                    }
                                } else {
                                    JSONObject paramObj = new JSONObject();
                                    paramObj.put("name", p.name());
                                    paramObj.put("type", p.type().getValue() + "[" + p.type().getText() + "]");
                                    paramObj.put("description", p.desc());
                                    outputList.add(paramObj);
                                }
                            }
                        }
                    } else if (anno.annotationType().equals(Description.class)) {
                        Description description = (Description) anno;
                        jsonObj.put("description", description.desc());
                    } else if (anno.annotationType().equals(Example.class)) {
                        Example example = (Example) anno;
                        String content = example.example();
                        if (StringUtils.isNotBlank(content)) {
                            try {
                                content = JSONObject.parseObject(content).toString();
                            } catch (Exception ex) {
                                try {
                                    content = JSONArray.parseArray(content).toString();
                                } catch (Exception ignored) {

                                }
                            }
                            jsonObj.put("example", content);
                        }
                    }
                }
            }
        } catch (NoSuchMethodException | SecurityException e) {
            logger.error(e.getMessage());
        }
        if (!outputList.isEmpty()) {
            jsonObj.put("output", outputList);
        }
        if (!inputList.isEmpty()) {
            jsonObj.put("input", inputList);
        }
        return jsonObj;
    }
}
