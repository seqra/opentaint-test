package opentaint.approximations.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;
import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.io.IOException;
import java.lang.reflect.Type;

@Approximate(com.google.gson.Gson.class)
public class Gson {

    public String toJson(Object src) {
        if (OpentaintNdUtil.nextBool()) return null;
        return (String) src;
    }

    public String toJson(Object src, Type typeOfSrc) {
        if (OpentaintNdUtil.nextBool()) return null;
        return (String) src;
    }

    public void toJson(Object src, Appendable writer) throws JsonIOException {
        append(writer, src);
    }

    public void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIOException {
        append(writer, src);
    }

    public void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIOException {
        writeJsonValue(writer, src);
    }

    public String toJson(JsonElement jsonElement) {
        if (OpentaintNdUtil.nextBool()) return null;
        return (String) (Object) jsonElement;
    }

    public void toJson(JsonElement jsonElement, Appendable writer) throws JsonIOException {
        append(writer, jsonElement);
    }

    public void toJson(JsonElement jsonElement, JsonWriter writer) throws JsonIOException {
        writeJsonValue(writer, jsonElement);
    }

    private void append(Appendable writer, Object value) throws JsonIOException {
        if (OpentaintNdUtil.nextBool()) return;
        try {
            writer.append((CharSequence) value);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    private void writeJsonValue(JsonWriter writer, Object value) throws JsonIOException {
        if (OpentaintNdUtil.nextBool()) return;
        try {
            writer.value((String) value);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }
}
