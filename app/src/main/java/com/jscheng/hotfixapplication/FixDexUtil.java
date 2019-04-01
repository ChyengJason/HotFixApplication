package com.jscheng.hotfixapplication;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created By Chengjunsen on 2019/4/1
 */
public class FixDexUtil {
    private static final String DEX_SUFFIX = ".dex";
    private static final String APK_SUFFIX = ".apk";
    private static final String JAR_SUFFIX = ".jar";
    private static final String ZIP_SUFFIX = ".zip";
    private static final String OPTIMIZE_DEX_DIR = "optimize_dex";

    public static final String DEX_DIR = "odex";

    public static void hotfix(Context context) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();

        // 遍历所有的修复dex , 因为可能是多个dex修复包
        File dirPath = externalStorageDirectory != null ?
                externalStorageDirectory :
                new File(context.getFilesDir(), DEX_DIR);// data/data/包名/files/odex（这个可以任意位置）

        HashSet<File> fiexFiles = getFixFiles(dirPath.getAbsolutePath());
        if (fiexFiles.isEmpty()) {
            Toast.makeText(context, "不需要修复，数量为空", Toast.LENGTH_SHORT).show();
        } else {
            doDexFix(context, fiexFiles);
        }
    }

    private static HashSet<File> getFixFiles(String path) {
        HashSet<File> loadedDex = new HashSet<>();
        File dir = new File(path);
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length <= 0) {
            return loadedDex;
        }

        for (File file : listFiles) {
            if (file.getName().startsWith("classes") && (file.getName().endsWith(DEX_SUFFIX)
                    || file.getName().endsWith(APK_SUFFIX)
                    || file.getName().endsWith(JAR_SUFFIX)
                    || file.getName().endsWith(ZIP_SUFFIX))) {
                loadedDex.add(file);
            }
        }
        return loadedDex;
    }

    private static void doDexFix(Context context, HashSet<File> loadedDex) {
        String optimizeDir = context.getFilesDir().getAbsolutePath() +
                File.separator + OPTIMIZE_DEX_DIR;
        // data/data/包名/files/optimize_dex（这个必须是自己程序下的目录）

        File fopt = new File(optimizeDir);
        if (!fopt.exists()) {
            fopt.mkdirs();
        }

        try {
            PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
            for (File dex : loadedDex) {
                DexClassLoader dexClassLoader = new DexClassLoader(
                        dex.getAbsolutePath(),
                        fopt.getAbsolutePath(),// 存放dex的解压目录（用于jar、zip、apk格式的补丁）
                        null, // 加载dex时需要的库
                        pathClassLoader // 父加载器
                );
                Object dexPathList = getPathList(dexClassLoader);
                Object pathPathList = getPathList(pathClassLoader);

                Object newDexElements = getDexElements(dexPathList);
                Object oldDexElements = getDexElements(pathPathList);

                Object dexElements = combineArray(newDexElements, oldDexElements);

                Object pathList = getPathList(pathClassLoader);
                setField(pathList, pathList.getClass(), "dexElements", dexElements);
            }
            Toast.makeText(context, "修复完成", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 反射得到pathList中的dexElements
     */
    private static Object getDexElements(Object pathList) throws NoSuchFieldException, IllegalAccessException {
        return getField(pathList, pathList.getClass(), "dexElements");
    }

    /**
     * 反射得到类加载器中的pathList对象
     */
    private static Object getPathList(ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(classLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    private static Object getField(Object obj, Class<?> cls, String field) throws NoSuchFieldException, IllegalAccessException {
        Field localField = cls.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }

    private static void setField(Object obj, Class<?> cls, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = cls.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(obj, value);
    }

    private static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> clazz = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);// 得到左数组长度（补丁数组）
        int j = Array.getLength(arrayRhs);// 得到原dex数组长度
        int k = i + j;// 得到总数组长度（补丁数组+原dex数组）
        Object result = Array.newInstance(clazz, k);// 创建一个类型为clazz，长度为k的新数组
        System.arraycopy(arrayLhs, 0, result, 0, i);
        System.arraycopy(arrayRhs, 0, result, i, j);
        return result;
    }
}
