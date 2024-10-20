package com.xws111;

import com.intellij.CodeStyleBundle;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * @Description: 导出器
 * @date: 2024/10/18 17:34
 * @author: xws111
 **/
public class Exporter {
    // 项目对象
    private final Project project;
    // 输出路径
    private final String outputDirectory;

    // 默认输出路径为桌面
    public Exporter(Project project) {
        this.project = project;
        this.outputDirectory = getDesktopDirectory();
    }

    // 传入参数作为输出路径
    public Exporter(Project project, String outputDirectory) {
        this.project = project;
        this.outputDirectory = outputDirectory;
    }

    /**
     * 判断文件是否是 .java 为扩展名的文件。
     *
     * @param virtualFile VirtualFile
     * @return 是的话返回 true，否则返回 false
     */
    public static boolean isJavaFile(VirtualFile virtualFile) {
        return virtualFile.getName().endsWith(".java");
    }

    /**
     * 获取文件类型。类型包括 Class、Interface、Enum、Annotation，返回字符串。
     *
     * @param psiFile PsiFile
     * @return 类型的字符串形式。
     */
    public static String getType(PsiFile psiFile) {
        if (!(psiFile instanceof PsiJavaFile javaFile)) {
            return "Not a valid Java file";
        }

        PsiClass[] classes = javaFile.getClasses();

        if (classes.length > 0) {
            PsiClass psiClass = classes[0]; // 拿到第一个 class 进行判断
            if (psiClass.isInterface()) {
                return "Interface";
            } else if (psiClass.isEnum()) {
                return "Enum";
            } else if (psiClass.isAnnotationType()) {
                return "Annotation";
            } else {
                return "Class";
            }
        }
        Messages.showMessageDialog("Can't resolve this java file.", "Error", Messages.getErrorIcon());
        return "Unknown type";
    }

    // 判断是否是Windows操作系统
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    // 判断是否是Mac操作系统
    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    // 判断是否是Linux操作系统
    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("nux");
    }

    /**
     * 根据系统获取桌面路径
     *
     * @return 桌面路径
     */
    public static String getDesktopDirectory() {
        String userHome = System.getProperty("user.home");
        String desktopPath;

        // 根据操作系统设置桌面路径
        if (isWindows()) {
            desktopPath = userHome + "\\Desktop";
        } else if (isMac()) {
            desktopPath = userHome + "/Desktop";
        } else if (isLinux()) {
            desktopPath = userHome + "/Desktop";  // 大多数Linux桌面环境也将桌面放在这里
        } else {
            throw new UnsupportedOperationException("Unsupported operating system.");
        }

        // 检查桌面路径是否存在
        File desktopDir = new File(desktopPath);
        if (!desktopDir.exists()) {
            throw new IllegalStateException("Desktop directory not found.");
        }

        return desktopPath;
    }

    /**
     * 解析格式，调整排版并输出到 md 文件中。
     *
     * @param file VirtualFile
     */
    public void exportToMarkdown(VirtualFile file) {
        // 获取 Psi 文件
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile instanceof PsiJavaFile javaFile) {
            // 处理文件
            String fileName = file.getName() + "源码笔记.md";
            String content = processFile(javaFile);
            writeToFile(fileName, content);
        } else {
            Messages.showInfoMessage("Not a valid Java file", "Error");
        }

    }

    /**
     * 写入到文件中
     *
     * @param fileName 文件名
     * @param content 内容
     */
    private void writeToFile(String fileName, String content) {
        try (FileWriter writer = new FileWriter(outputDirectory + "/" + fileName)) {
            writer.write(content);
            Messages.showInfoMessage("输出到：" + outputDirectory, "Info");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据类型处理文件
     * @param psiJavaFile 要处理的文件
     * @return 返回处理结果，也就是文档内容
     */
    private String processFile(PsiJavaFile psiJavaFile) {
        String classType = getType(psiJavaFile);
        String content = "";
        switch (classType) {
            case "Class", "Interface" -> content = processClassFile(psiJavaFile);
            // todo 注解和枚举类的处理
            case "Enum" -> Messages.showInfoMessage("Not a class or interface", "Error");
            case "Annotation" -> Messages.showInfoMessage("Not a class or interface", "Error");
            default -> Messages.showInfoMessage("Not a class or interface", "Error");
        }

        return content;
    }

    /**
     * 处理类文件
     * @param psiJavaFile 要处理的类文件
     * @return 返回文档总体内容
     */
    private String processClassFile(PsiJavaFile psiJavaFile) {
        StringBuilder stringBuilder = new StringBuilder();

        // 添加标题
        stringBuilder.append("# 【在此处输入标题】：").append(psiJavaFile.getName()).append("\n");

        // 添加前言
        stringBuilder.append("## 前言").append("\n").append("在此处添加前言...\n");

        // 添加类说明
        for (PsiClass psiClass : psiJavaFile.getClasses()) {
           appendClassInfo(psiClass, stringBuilder);
        }

        // 添加总结
        stringBuilder.append("## 总结").append("\n").append("在此处添加总结...");
        return stringBuilder.toString();
    }

    /**
     * 去掉类注释里面的 / 和 * 符号，并将重点字符高亮
     *
     * @param owner 原始注释
     * @return 去掉符号后的纯文本
     */
    public static String extractCommentText(PsiDocCommentOwner owner) {
        if (owner.getDocComment() == null) {
            return "";
        }

        // 分割行，去掉第一行和最后一行
        String[] lines = owner.getDocComment().getText().split("\n");
        StringBuilder extractedComment = new StringBuilder();
        boolean isParagraphOpen = false;

        // 从第二行开始遍历，直到倒数第二行
        for (int i = 1; i < lines.length - 1; i++) {
            String line = lines[i];

            // 去掉每行开头的 * 和一个空格
            line = line.replaceFirst("\\s*\\*\\s?", "").trim();

            // 如果这一行是空的，并且当前段落已经开始，则结束这个段落，防止出现 md 文档 出现多重引用
            if (line.isEmpty()) {
                if (isParagraphOpen) {
                    extractedComment.append("\n\n");
                    isParagraphOpen = false;
                }
                continue; // 跳过空行
            }

            // 如果段落尚未开始，添加引用符号并标记段落已开始，因为有些源代码分段写的
            if (!isParagraphOpen) {
                extractedComment.append("> ");
                isParagraphOpen = true;
            }

            // {@code ...} 为蓝色加粗文本
            line = line.replaceAll("\\{@code (.+?)\\}", "<span style=\"color: blue; font-weight: bold;\">$1</span>");

            // {@link ...} 为蓝色加粗文本，且只保留字母
            line = line.replaceAll("\\{@link [^}]*\\b(\\w+)\\}", "<span style=\"color: blue; font-weight: bold;\">$1</span>");

            // 删除 <a> 和 <p> 标签，保留中间内容
            line = line.replaceAll("</?a[^>]*>", "");
            line = line.replaceAll("</?p[^>]*>", "");

            // <em> 为斜体红色加粗文本
            line = line.replaceAll("<em>(.+?)</em>", "<span style=\"color: red; font-style: italic; font-weight: bold;\">$1</span>");
            line = line.replaceAll("<i>(.+?)</i>", "<span style=\"color: red; font-style: italic; font-weight: bold;\">$1</span>");

            // 如果以 @author、@see 或 @since 等开头，单独一行
            if (line.startsWith("@")) {
                if (isParagraphOpen) {
                    extractedComment.append("\n"); // 结束前一段内容
                    isParagraphOpen = false;
                }
                extractedComment.append(line).append("\n"); // 单独放置该行
                continue; // 跳过后续处理，避免在同一行再添加引用符
            }
            // 处理代码块之间的空白
            extractedComment.append(line).append(" ");
        }

        // 返回处理后的注释文本
        return extractedComment.append("\n").toString();
    }


    /**
     * 添加类信息。1. 添加 2 级标题为类名。 2. 添加 javaDoc 注释。 3. 添加字段。 4. 添加方法。
     * @param psiClass PsiClass
     * @param stringBuilder StringBuilder
     */
    private void appendClassInfo(PsiClass psiClass, StringBuilder stringBuilder) {
        // 添加类
        if (psiClass.isInterface()) {
            stringBuilder.append("## ").append(psiClass.getName()).append(" 接口").append("\n");
        } else {
            stringBuilder.append("## ").append(psiClass.getName()).append(" 类").append("\n");
        }
        // 添加 JavaDoc 注释
        stringBuilder.append(extractCommentText(psiClass));


        // 添加字段
        if (psiClass.getFields().length > 0) {
            stringBuilder.append("### ").append("属性").append("\n");
        }
        for (PsiField field : psiClass.getFields()) {
            appendFieldInfo(field, stringBuilder);
        }
        // 添加方法
        if (psiClass.getMethods().length > 0) {
            stringBuilder.append("### ").append("方法").append("\n");
        }
        for (PsiMethod method : psiClass.getMethods()) {
            appendMethodInfo(method, stringBuilder);
        }

        // 添加内部类
        for (PsiClass clazz : psiClass.getInnerClasses()) {
            appendClassInfo(clazz, stringBuilder);
        }
    }


    /**
     * 添加字段。1. 添加字段 4 级标题为字段名。2. 添加 javaDoc 注释。 3. 添加字段源代码
     * @param field PsiField
     * @param stringBuilder StringBuilder
     */
    private void appendFieldInfo(PsiField field, StringBuilder stringBuilder) {
        stringBuilder.append("#### ").append(field.getName()).append("\n");

        // 添加注释
        stringBuilder.append(extractCommentText(field)).append("\n");
        // 提取代码，如果是注释则跳过
        stringBuilder.append("```java");
        for (PsiElement element : field.getChildren()) {
            // 跳过 Javadoc 注释部分
            if (element instanceof PsiDocComment) {
                continue;
            }
            // 如果 StringBuilder 的以 ```java 结尾，并且当前元素不是以 \n 开头，则加上一个换行符
            // todo: 不知道为什么内部类，PsiElement 中，代码之前没有换行符，可能是没注释的原因。
            if (stringBuilder.length() >= 7) {
                // 获取最后 7 个字符
                String lastSevenChars = stringBuilder.substring(stringBuilder.length() - 7);
                if (lastSevenChars.equals("```java") && !element.getText().startsWith("\n")) {
                    stringBuilder.append("\n");
                }
            }

            stringBuilder.append(element.getText());
        }

        stringBuilder.append("\n```\n");

    }


    /**
     * 添加方法。1. 添加 4 级标题为方法名。 2. 添加方法的 JavaDoc 注释。 3. 添加方法源代码
     * @param method 要添加的方法
     * @param stringBuilder stringBuilder
     */
    private void appendMethodInfo(PsiMethod method, StringBuilder stringBuilder) {
        // 添加方法名
        stringBuilder.append("#### ").append(method.getName()).append("\n");
        // 添加注释
        appendDocCommentText(method, stringBuilder);
        // 添加方法源代码
        appendCodeBlock(method, stringBuilder);
    }

    /**
     * 添加 javaDoc 注释文本。将处理后的注释文本，添加到 stringBuilder。
     * @param psiDocCommentOwner 拥有 javaDoc 注释的类
     */
    private void appendDocCommentText(PsiDocCommentOwner psiDocCommentOwner, StringBuilder stringBuilder) {
        PsiDocComment comment = psiDocCommentOwner.getDocComment();
        if (comment != null) {
            stringBuilder.append(extractCommentText(psiDocCommentOwner)).append("\n");
        }
    }

    /**
     * 添加方法体。 将方法源代码添加到 stringBuilder。
     * @param method 要添加的方法
     * @param stringBuilder stringBuilder
     */
    private void appendCodeBlock(PsiMethod method, StringBuilder stringBuilder) {
        stringBuilder.append("```java\n\t");
        if (method.getContainingClass().getContainingClass() != null) {
            stringBuilder.append("\t");
        }
        StringBuilder codeBlock = new StringBuilder();

        codeBlock.append(method.getModifierList().getText()).append(" ");
        codeBlock.append(method.getReturnTypeElement() != null ? method.getReturnTypeElement().getText() + " " : "");
        codeBlock.append(method.getName());

        // 2. 添加参数列表
        codeBlock.append(method.getParameterList().getText());

        // 3. 添加方法体
        PsiCodeBlock body = method.getBody();
        if (body != null) {
            codeBlock.append(" ").append(body.getText());
        } else {
            codeBlock.append(";"); // 如果是抽象方法或接口方法
        }

        stringBuilder.append(codeBlock);

        //PsiFile tempFile = PsiFileFactory.getInstance(method.getProject()).createFileFromText("temp.java", JavaLanguage.INSTANCE, codeBlock.toString());
        //CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(method.getProject());
        //stringBuilder.append(codeStyleManager.reformat(tempFile).getText().trim());

        stringBuilder.append("\n```\n");


    }
}
