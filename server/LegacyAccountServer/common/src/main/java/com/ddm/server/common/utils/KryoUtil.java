package com.ddm.server.common.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Kryo 序列化
 *
 * @author huaxing
 */
public class KryoUtil {

    private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // 支持对象循环引用（否则会栈溢出）
            kryo.setReferences(true);
            // 不强制要求注册类（注册行为无法保证多个 JVM 内同一个类的注册编号相同；而且业务系统中大量的 Class 也难以一一注册）
            kryo.setRegistrationRequired(false);
            return kryo;
        }
    };

    private static final ThreadLocal<Output> outs = ThreadLocal.withInitial(() -> new Output(1024, 1024 * 1024));
    private static final ThreadLocal<Input> ins = ThreadLocal.withInitial(() -> {
        return new Input();
    });

    /**
     * Serializer
     *
     * @return
     */
    public static  <T> byte[] Serializer(T obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); Output out = outs.get();) {
            out.setOutputStream(bos);
            kryoLocal.get().writeClassAndObject(out, obj);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            CommLogD.error("Kryo serialization failed", e);
        } finally {
            outs.remove();
            kryoLocal.remove();
        }
        return null;
    }




    /**
     * 序列化
     *
     * @return
     */
    public static <T> ByteBuffer SerializerByteBuffer(T object) {
        return ByteBuffer.wrap(Serializer(object));
    }

    /**
     * 反序列化
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T Deserializer(byte[] b) {
        try (Input in = ins.get()) {
            in.setBuffer(b, 0, b.length);
            return (T) kryoLocal.get().readClassAndObject(in);
        } finally {
            ins.remove();
            kryoLocal.remove();
        }
    }

    /**
     * 反序列化
     *
     * @return
     */
    public static <T> T DeserializerByteBuffer(ByteBuffer binary) {
        byte[] bytes = new byte[binary.capacity()];
        binary.get(bytes);
        return Deserializer(bytes);
    }

}
