package com.example.curs;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import jglm.Mat;

public class Model {

    private FloatBuffer vertexBuffer, normalBuffer, textureBuffer;
    private ShortBuffer indexBuffer;
    private int indexCount;
    private int program;
    int textureID;
    int textureNum;
    float[] color = new float[4];
    boolean smooth;

    private float[] MVPMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    //Создание программы и линковка
    private void CreateProgram(String vertexShaderCode, String fragmentShaderCode){
        int vertexShader = MyRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        GLES20.glCompileShader(vertexShader);
        GLES20.glCompileShader(fragmentShader);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glAttachShader(program, vertexShader);

        GLES20.glLinkProgram(program);
    }

    public Model(Context context, String filename, int textureID, int textureNum, float[] translate, float scale, boolean smooth){
        this.smooth = smooth;
        this.textureID = textureID;
        this.textureNum = textureNum;

        Matrix.setIdentityM(modelMatrix, 0);
        //Смещаем объект на переданные значения
        Matrix.translateM(modelMatrix, 0, translate[0], translate[1], translate[2]);
        //Масштабируем объект по переданному значению
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        //СЧИТЫВАНИЕ .OBJ ФАЙЛА В БУФФЕРЫ
        readOBJFile(context, filename);

        //ШЕЙДЕРЫ/////////////////////
        String vertexShaderCode =
                "precision mediump float;\n"+
                "varying vec2 v_TexCord;\n" +
                "varying vec3 v_Normal;\n" +
                "varying vec3 v_LightPos, v_EyePos;\n"+

                "uniform mat4 u_MVPMatrix;\n" +
                "uniform vec3 u_LightPos, u_EyePos;\n"+

                "attribute vec3 a_Position;\n" +
                "attribute vec2 a_TexCord;\n" +
                "attribute vec3 a_Normal;\n" +

                "void main(){\n" +
                    "v_TexCord = a_TexCord;\n" +
                    "v_Normal = a_Normal;\n" +
                    "v_LightPos = u_LightPos - a_Position;\n" +
                    "v_EyePos = u_EyePos - a_Position;\n" +
                    "gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);\n" +
                "}\n";

        String fragmentShaderCode =
                "precision mediump float;\n" +

                "varying vec2 v_TexCord;\n" +
                "varying vec3 v_Normal;\n" +
                "varying vec3 v_LightPos, v_EyePos;\n"+

                "uniform sampler2D u_Texture;\n" +

                "void main() {\n" +
                    "float specularPower=14.0;\n"+
                    "vec4 diffuseColor = texture2D(u_Texture, v_TexCord);\n" +
                    "vec4 specularColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                    "vec3 normal = normalize(v_Normal);\n" +
                    "vec3 light = normalize(v_LightPos);\n" +
                    "vec3 reflection = reflect(-light, normal);\n" +
                    "vec4 diffuse = diffuseColor * max(dot(normal, light), 0.0);\n" +
                    "vec4 specular = specularColor * pow(max(dot(normalize(v_EyePos), reflection), 0.0), specularPower);\n" +
                    "gl_FragColor = diffuse + specular;\n" +
                "}\n";

        //Передаём шейдеры в программу
        CreateProgram(vertexShaderCode, fragmentShaderCode);
    }

    public Model(Context context, String filename, float[] newColor, float[] translate, float scale, boolean smooth){
        color = newColor;
        this.smooth = smooth;

        Matrix.setIdentityM(modelMatrix, 0);
        //Смещаем объект на переданные значения
        Matrix.translateM(modelMatrix, 0, translate[0], translate[1], translate[2]);
        //Масштабируем объект по переданному значению
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        //СЧИТЫВАНИЕ .OBJ ФАЙЛА В БУФФЕРЫ
        readOBJFile(context, filename);

        //ШЕЙДЕРЫ/////////////////////
        String vertexShaderCode =
                "precision mediump float;\n"+
                "varying vec2 v_TexCord;\n" +
                "varying vec3 v_Normal;\n" +
                "varying vec3 v_LightPos, v_EyePos;\n"+

                "uniform mat4 u_MVPMatrix;\n" +
                "uniform vec3 u_LightPos, u_EyePos;\n"+

                "attribute vec3 a_Position;\n" +
                "attribute vec2 a_TexCord;\n" +
                "attribute vec3 a_Normal;\n" +

                "void main(){\n" +
                "v_TexCord = a_TexCord;\n" +
                "v_Normal = a_Normal;\n" +
                "v_LightPos = u_LightPos - a_Position;\n" +
                "v_EyePos = u_EyePos - a_Position;\n" +
                "gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);\n" +
                "}\n";

        String fragmentShaderCode =
                "precision mediump float;\n" +

                "varying vec2 v_TexCord;\n" +
                "varying vec3 v_Normal;\n" +
                "varying vec3 v_LightPos, v_EyePos;\n"+

                "uniform vec4 u_Color;\n" +

                "void main() {\n" +
                "float specularPower=8.0;\n"+
                "vec4 diffuseColor = u_Color;\n" +
                "vec4 specularColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "vec3 normal = normalize(v_Normal);\n" +
                "vec3 light = normalize(v_LightPos);\n" +
                "vec3 reflection = reflect(-light, normal);\n" +
                "vec4 diffuse = diffuseColor * max(dot(normal, light), 0.0);\n" +
                "vec4 specular = specularColor * pow(max(dot(normalize(v_EyePos), reflection), 0.0), specularPower);\n" +
                "vec4 finalColor = diffuse + specular;\n" +
                "if (u_Color.w < 1.0) finalColor += u_Color;\n" +
                "gl_FragColor = finalColor;\n" +
                "}\n";

        //Передаём шейдеры в программу
        CreateProgram(vertexShaderCode, fragmentShaderCode);
    }

    void draw(float[] VPMatrix, float[] eyePos, boolean isTextured){
        //Перемножаем переданную матрицу с модельной матрицой, в которой мы задали смещение и масштаб
        Matrix.multiplyMM(MVPMatrix, 0, VPMatrix, 0, modelMatrix, 0);

        //Используем шейдеры
        GLES20.glUseProgram(program);

        //Если используем текстуры
        int textureHandle = 0;

        if(isTextured){
            textureHandle = GLES20.glGetAttribLocation(program, "a_TexCord");
            int textureLocation = GLES20.glGetUniformLocation(program, "u_Texture");

            //Передаём буффер текстурных вершин
            GLES20.glEnableVertexAttribArray(textureHandle);
            GLES20.glVertexAttribPointer(textureHandle, 2,GLES20.GL_FLOAT, false, 0, textureBuffer);

            //Активируем текстуру
            GLES20.glActiveTexture(textureNum);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
            GLES20.glUniform1i(textureLocation, textureNum - GLES20.GL_TEXTURE0);
        }
        //Если задаём цвет фигуре
        else{
            int colorHandle = GLES20.glGetUniformLocation(program, "u_Color");
            GLES20.glUniform4f(colorHandle, color[0], color[1], color[2], color[3]);
        }

        //Определяем заголовки для последующего связывания переменных с шейдерами
        int positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
        int normalHandle = GLES20.glGetAttribLocation(program, "a_Normal");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        int eyeHandle = GLES20.glGetUniformLocation(program, "u_EyePos");
        int lightHandle = GLES20.glGetUniformLocation(program, "u_LightPos");

        //Передаём позицию камеры
        GLES20.glUniform3f(eyeHandle, eyePos[0], eyePos[1], eyePos[2]);

        //Передаём позицию источника света
        GLES20.glUniform3f(lightHandle, 0, 15, 0);

        //Передаём буффер вершин
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        //Передаём буффер нормалей
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        //Передаём матрицу проекции
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, MVPMatrix, 0);

        //Отрисовываем фигуру используя буффер индексов
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        //Отключаем передачу массивов в атрибуты
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        if(isTextured)
            GLES20.glDisableVertexAttribArray(textureHandle);
    }

    private void readOBJFile(Context context, String filename){
        ArrayList<Float> verticies = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<float[]> textureCords = new ArrayList<>();
        ArrayList<Short> indices = new ArrayList<>();
        ArrayList<Short> normalIndices = new ArrayList<>();
        ArrayList<Short> textureIndices = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(filename), "UTF-8"));

            String mLine;
            //Считываем строку .obj файла
            while ((mLine = reader.readLine()) != null) {
                String[] parts = mLine.split(" ");

                switch (parts[0].trim()){
                    //Считывание вершин
                    case "v":
                        for(int i = 1; i < parts.length; i++){
                            Float vertex = Float.valueOf(parts[i].trim());
                            verticies.add(vertex);
                        }
                        break;

                    //Считывание нормалей
                    case "vn":
                        for(int i = 1; i < parts.length; i++){
                            Float normal = Float.valueOf(parts[i].trim());
                            normals.add(normal);
                        }
                        break;

                    //Считывание текстурных вершин
                    case "vt":
                        float[] texArr = new float[2];
                        texArr[0] = Float.valueOf(parts[1].trim());
                        texArr[1] = Float.valueOf(parts[2].trim());

                        textureCords.add(texArr);
                        break;

                    //Считывание граней (индексы вершин, индексы текстур, индексы нормалей)
                    case "f":
                        //Считывание индексов вершин
                        String[] faceParts = parts[1].split("/");
                        Short index = Short.valueOf(faceParts[0].trim());
                        indices.add((short)(index - 1));

                        faceParts = parts[2].split("/");
                        index = Short.valueOf(faceParts[0].trim());
                        indices.add((short)(index - 1));

                        faceParts = parts[3].split("/");
                        index = Short.valueOf(faceParts[0].trim());
                        indices.add((short)(index - 1));

                        //Считывание индексов текстур
                        faceParts = parts[1].split("/");
                        index = Short.valueOf(faceParts[1].trim());
                        textureIndices.add((short)(index - 1));

                        faceParts = parts[2].split("/");
                        index = Short.valueOf(faceParts[1].trim());
                        textureIndices.add((short)(index - 1));

                        faceParts = parts[3].split("/");
                        index = Short.valueOf(faceParts[1].trim());
                        textureIndices.add((short)(index - 1));

                        //Считывание индексов нормалей
                        faceParts = parts[1].split("/");
                        index = Short.valueOf(faceParts[2].trim());
                        normalIndices.add((short)(index - 1));

                        faceParts = parts[2].split("/");
                        index = Short.valueOf(faceParts[2].trim());
                        normalIndices.add((short)(index - 1));

                        faceParts = parts[3].split("/");
                        index = Short.valueOf(faceParts[2].trim());
                        normalIndices.add((short)(index - 1));
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        short[] indexArray = new short[indices.size()];
        float[] vertexArray = new float[indices.size()*3];
        float[] textureArray = new float[indices.size()*2];
        float[] normalArray = new float[indices.size()*3];

        float[] normalValue = new float[verticies.size()];
        float[] normalCount = new float[verticies.size()];

        for(int i = 0; i < verticies.size(); i++) {
            normalValue[i] = 0;
            normalCount[i] = 0;
        }

        //Заполнение массивов значениями из списков
        for(int i=0; i<indices.size(); ++i) {
            int index = indices.get(i);
            int textureIndex = textureIndices.get(i);
            int normalIndex = normalIndices.get(i);

            vertexArray[i*3] = verticies.get(index*3);
            vertexArray[i*3 + 1] = verticies.get(index*3 + 1);
            vertexArray[i*3 + 2] = verticies.get(index*3 + 2);

            //Если сглаживание отключено, записываем нормали из файла
            if(!smooth){
                normalArray[i*3] = normals.get(normalIndex*3);
                normalArray[i*3 + 1] = normals.get(normalIndex*3 + 1);
                normalArray[i*3 + 2] = normals.get(normalIndex*3 + 2);
            }

            //Иначе будем рассчитывать среднее значение нормалей
            else{
                normalValue[index*3] += normals.get(normalIndex*3);
                normalValue[index*3 + 1] += normals.get(normalIndex*3 + 1);
                normalValue[index*3 + 2] += normals.get(normalIndex*3 + 2);

                normalCount[index*3] += 1;
                normalCount[index*3 + 1] += 1;
                normalCount[index*3 + 2] += 1;
            }

            textureArray[i*2] = textureCords.get(textureIndex)[0];
            textureArray[i*2+1] = textureCords.get(textureIndex)[1];

            indexArray[i] = (short)i;
        }

        //Среднее значение нормали (сглаживание)
        if(smooth){
            for (int i = 0; i < indices.size(); i++) {
                int index = indices.get(i);

                normalArray[i*3] = normalValue[index*3] / normalCount[index*3];
                normalArray[i*3 + 1] = normalValue[index*3 + 1] / normalCount[index*3 + 1];
                normalArray[i*3 + 2] = normalValue[index*3 + 2] / normalCount[index*3 + 2];
            }
        }

        //Сортировка индексов
        indexArray = sortIndices(vertexArray, indexArray);

        //Преобразование массивов в буфферы
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertexArray.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = vertexByteBuffer.asFloatBuffer();
        vertexBuffer.put(vertexArray);
        vertexBuffer.position(0);

        ByteBuffer indexByteBuffer = ByteBuffer.allocateDirect(indexArray.length * 2);
        indexByteBuffer.order(ByteOrder.nativeOrder());
        indexBuffer = indexByteBuffer.asShortBuffer();
        indexBuffer.put(indexArray);
        indexBuffer.position(0);

        ByteBuffer normalByteBuffer = ByteBuffer.allocateDirect(normalArray.length * 4);
        normalByteBuffer.order(ByteOrder.nativeOrder());
        normalBuffer = normalByteBuffer.asFloatBuffer();
        normalBuffer.put(normalArray);
        normalBuffer.position(0);

        ByteBuffer textureByteBuffer = ByteBuffer.allocateDirect(textureArray.length * 4);
        textureByteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = textureByteBuffer.asFloatBuffer();
        textureBuffer.put(textureArray);
        textureBuffer.position(0);

        indexCount = indices.size();
    }

    //Сортируем индексы вершин по высоте в вершинах
    private short[] sortIndices(float[] vertexArray, short[] indexArray){
        ArrayList<Float[]> indexOfIndex = new ArrayList<>();

        //Заполняем индексы индексов, суммируя высоты
        for (short i = 0; i < indexArray.length / 3; i++){
            Float[] tmp = new Float[]{0f, (float)i};
            for(int j = 0; j < 3; j++){
                tmp[0] += vertexArray[indexArray[(i * 3) + j] * 3 + 1];
            }
            indexOfIndex.add(tmp);
        }

        //Сортировка
        Collections.sort(indexOfIndex, new Comparator<Float[]>() {
            @Override
            public int compare(Float[] o1, Float[] o2) {
                return Float.compare(o1[0], o2[0]);
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        });

        short[] tmpArr = new short[indexArray.length];

        //Приводим к обычному виду
        for (int i = 0; i < indexOfIndex.size(); i++){
            for (int j = 0; j < 3; j++){
                tmpArr[(i * 3) + j] = indexArray[(int)(float)indexOfIndex.get(i)[1] * 3 + j];
            }
        }

        return tmpArr;
    }
}
