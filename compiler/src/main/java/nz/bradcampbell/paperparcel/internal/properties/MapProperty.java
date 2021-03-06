package nz.bradcampbell.paperparcel.internal.properties;

import com.squareup.javapoet.*;
import nz.bradcampbell.paperparcel.internal.Property;
import nz.bradcampbell.paperparcel.internal.utils.PropertyUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static nz.bradcampbell.paperparcel.internal.utils.PropertyUtils.createProperty;
import static nz.bradcampbell.paperparcel.internal.utils.PropertyUtils.literal;

public class MapProperty extends Property {
  public MapProperty(Property.Type propertyType, boolean isNullable, String name) {
    super(propertyType, isNullable, name);
  }

  @Override protected CodeBlock readFromParcelInner(CodeBlock.Builder block, ParameterSpec in, @Nullable FieldSpec classLoader) {
    Property.Type propertyType = getPropertyType();
    Property.Type keyType = propertyType.getChildType(0);
    Property.Type valueType = propertyType.getChildType(1);

    // Read size
    String mapSize = getName() + "Size";
    block.addStatement("$T $N = $N.readInt()", int.class, mapSize, in);

    // Create map to read into
    String mapName = getName();

    TypeName typeName = propertyType.getWildcardTypeName();
    if (typeName instanceof WildcardTypeName) {
      typeName = ((WildcardTypeName) typeName).upperBounds.get(0);
    }

    boolean patchedTypeName = false;

    TypeName keyTypeName = keyType.getWildcardTypeName();
    if (keyTypeName instanceof WildcardTypeName) {
      keyTypeName = ((WildcardTypeName) keyTypeName).upperBounds.get(0);
      patchedTypeName = true;
    }

    TypeName valueTypeName = valueType.getWildcardTypeName();
    if (valueTypeName instanceof WildcardTypeName) {
      valueTypeName = ((WildcardTypeName) valueTypeName).upperBounds.get(0);
      patchedTypeName = true;
    }

    if (patchedTypeName) {
      ParameterizedTypeName originalType = (ParameterizedTypeName) typeName;
      typeName = ParameterizedTypeName.get(originalType.rawType, keyTypeName, valueTypeName);
    }

    if (propertyType.isInterface()) {
      block.addStatement("$T $N = new $T<$T, $T>($N)", typeName, mapName, HashMap.class, keyTypeName, valueTypeName, mapSize);
    } else {
      block.addStatement("$T $N = new $T()", typeName, mapName, typeName);
    }

    // Write a loop to iterate through each entity
    String indexName = getName() + "Index";
    block.beginControlFlow("for (int $N = 0; $N < $N; $N++)", indexName, indexName, mapSize, indexName);

    String keyName = getName() + "Key";
    String valueName = getName() + "Value";

    // Read in the key. Set isNullable to true as I don't know how to tell if a parameter is
    // nullable or not. Kotlin can do this, Java can't.
    CodeBlock keyLiteral = PropertyUtils.createProperty(keyType, keyName).readFromParcel(block, in, classLoader);

    // Read in the value. Set isNullable to true as I don't know how to tell if a parameter is
    // nullable or not. Kotlin can do this, Java can't.
    CodeBlock valueLiteral = PropertyUtils.createProperty(valueType, valueName).readFromParcel(block, in, classLoader);

    // Add the parameter to the output map
    block.addStatement("$N.put($L, $L)", mapName, keyLiteral, valueLiteral);

    block.endControlFlow();

    return literal("$N", mapName);
  }

  @Override protected void writeToParcelInner(CodeBlock.Builder block, ParameterSpec dest, CodeBlock sourceLiteral) {
    Property.Type propertyType = getPropertyType();

    // Write size
    block.addStatement("$N.writeInt($L.size())", dest, sourceLiteral);

    Property.Type keyType = propertyType.getChildType(0);
    TypeName keyTypeName = keyType.getWildcardTypeName();
    Property.Type valueType = propertyType.getChildType(1);
    TypeName valueTypeName = valueType.getWildcardTypeName();

    // Write a loop to iterate through each entry
    String entryName = getName() + "Entry";
    block.beginControlFlow("for ($T<$T, $T> $N : $L.entrySet())", Map.Entry.class, keyTypeName, valueTypeName,
        entryName, sourceLiteral);

    String keyName = getName() + "Key";
    CodeBlock keySourceLiteral = literal("$N.getKey()", entryName);
    String valueName = getName() + "Value";
    CodeBlock valueSourceLiteral = literal("$N.getValue()", entryName);

    // Write in the key. Set isNullable to true as I don't know how to tell if a parameter is
    // nullable or not. Kotlin can do this, Java can't.
    PropertyUtils.createProperty(keyType, keyName).writeToParcel(block, dest, keySourceLiteral);

    // Write in the value. Set isNullable to true as I don't know how to tell if a parameter is
    // nullable or not. Kotlin can do this, Java can't.
    PropertyUtils.createProperty(valueType, valueName).writeToParcel(block, dest, valueSourceLiteral);

    block.endControlFlow();
  }
}
