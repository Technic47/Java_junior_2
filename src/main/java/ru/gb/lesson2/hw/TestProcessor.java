package ru.gb.lesson2.hw;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TestProcessor {
    private static Method beforeEach = null;
    private static Method afterEach = null;
    private static Method beforeAll = null;
    private static Method afterAll = null;

    /**
     * Данный метод находит все void методы без аргументов в классе, и запускеет их.
     * <p>
     * Для запуска создается тестовый объект с помощью конструткора без аргументов.
     */
    public static void runTest(Class<?> testClass) {
        final Constructor<?> declaredConstructor;
        try {
            declaredConstructor = testClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
        }

        final Object testObj;
        try {
            testObj = declaredConstructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
        }

        List<Method> methods = new ArrayList<>();


        for (Method method : testClass.getDeclaredMethods()) {
            if (!checkForBeforeAfterAll(method)) {
                if (!checkForBeforeAfterEach(method)) {
                    if (method.isAnnotationPresent(Test.class)) {
                        checkTestMethod(method);
                        methods.add(method);
                    }
                }
            }

        }
        runAllTests(methods, testObj);
    }

    private static void runAllTests(List<Method> methods, Object testObj) {
        try {
            if (beforeAll != null) beforeAll.invoke(testObj);
            methods.stream()
                    .filter(m -> !m.isAnnotationPresent(Skip.class))
                    .filter(m -> !m.getAnnotation(Test.class).skip())
                    .sorted(Comparator.comparingInt(m -> m.getAnnotation(Test.class).order()))
                    .forEach(it -> runTest(it, testObj));
            if (afterAll != null) afterAll.invoke(testObj);
        } catch (Exception e) {
            throw new RuntimeException("Что-то пошло не так.");
        }
    }

    private static boolean checkForBeforeAfterAll(Method method) {
        boolean ok = false;
        if (method.isAnnotationPresent(BeforeAll.class)
                || method.isAnnotationPresent(AfterAll.class)) {
            if (method.isAnnotationPresent(BeforeAll.class)) {
                beforeAll = method;
            } else afterAll = method;
            ok = true;
        }
        return ok;
    }

    private static boolean checkForBeforeAfterEach(Method method) {
        boolean ok = false;
        if (method.isAnnotationPresent(BeforeEach.class)
                || method.isAnnotationPresent(AfterEach.class)) {
            if (method.isAnnotationPresent(AfterEach.class)) {
                afterEach = method;
            } else beforeEach = method;
            ok = true;
        }
        return ok;
    }

    private static void checkTestMethod(Method method) {
        if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
            throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
        }
    }

    private static void runTest(Method testMethod, Object testObj) {
        try {
            if (beforeEach != null) beforeEach.invoke(testObj);

            testMethod.invoke(testObj);

            if (afterEach != null) afterEach.invoke(testObj);

        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
        }
    }
}
