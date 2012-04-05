/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package edu.berkeley.cs.amplab.carat.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Registration implements org.apache.thrift.TBase<Registration, Registration._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Registration");

  private static final org.apache.thrift.protocol.TField UU_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("uuId", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField TIMESTAMP_FIELD_DESC = new org.apache.thrift.protocol.TField("timestamp", org.apache.thrift.protocol.TType.DOUBLE, (short)2);
  private static final org.apache.thrift.protocol.TField PLATFORM_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("platformId", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField SYSTEM_VERSION_FIELD_DESC = new org.apache.thrift.protocol.TField("systemVersion", org.apache.thrift.protocol.TType.STRING, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new RegistrationStandardSchemeFactory());
    schemes.put(TupleScheme.class, new RegistrationTupleSchemeFactory());
  }

  public String uuId; // required
  public double timestamp; // optional
  public String platformId; // optional
  public String systemVersion; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    UU_ID((short)1, "uuId"),
    TIMESTAMP((short)2, "timestamp"),
    PLATFORM_ID((short)3, "platformId"),
    SYSTEM_VERSION((short)4, "systemVersion");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // UU_ID
          return UU_ID;
        case 2: // TIMESTAMP
          return TIMESTAMP;
        case 3: // PLATFORM_ID
          return PLATFORM_ID;
        case 4: // SYSTEM_VERSION
          return SYSTEM_VERSION;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __TIMESTAMP_ISSET_ID = 0;
  private BitSet __isset_bit_vector = new BitSet(1);
  private _Fields optionals[] = {_Fields.TIMESTAMP,_Fields.PLATFORM_ID,_Fields.SYSTEM_VERSION};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.UU_ID, new org.apache.thrift.meta_data.FieldMetaData("uuId", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.TIMESTAMP, new org.apache.thrift.meta_data.FieldMetaData("timestamp", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.PLATFORM_ID, new org.apache.thrift.meta_data.FieldMetaData("platformId", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.SYSTEM_VERSION, new org.apache.thrift.meta_data.FieldMetaData("systemVersion", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Registration.class, metaDataMap);
  }

  public Registration() {
  }

  public Registration(
    String uuId)
  {
    this();
    this.uuId = uuId;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Registration(Registration other) {
    __isset_bit_vector.clear();
    __isset_bit_vector.or(other.__isset_bit_vector);
    if (other.isSetUuId()) {
      this.uuId = other.uuId;
    }
    this.timestamp = other.timestamp;
    if (other.isSetPlatformId()) {
      this.platformId = other.platformId;
    }
    if (other.isSetSystemVersion()) {
      this.systemVersion = other.systemVersion;
    }
  }

  public Registration deepCopy() {
    return new Registration(this);
  }

  @Override
  public void clear() {
    this.uuId = null;
    setTimestampIsSet(false);
    this.timestamp = 0.0;
    this.platformId = null;
    this.systemVersion = null;
  }

  public String getUuId() {
    return this.uuId;
  }

  public Registration setUuId(String uuId) {
    this.uuId = uuId;
    return this;
  }

  public void unsetUuId() {
    this.uuId = null;
  }

  /** Returns true if field uuId is set (has been assigned a value) and false otherwise */
  public boolean isSetUuId() {
    return this.uuId != null;
  }

  public void setUuIdIsSet(boolean value) {
    if (!value) {
      this.uuId = null;
    }
  }

  public double getTimestamp() {
    return this.timestamp;
  }

  public Registration setTimestamp(double timestamp) {
    this.timestamp = timestamp;
    setTimestampIsSet(true);
    return this;
  }

  public void unsetTimestamp() {
    __isset_bit_vector.clear(__TIMESTAMP_ISSET_ID);
  }

  /** Returns true if field timestamp is set (has been assigned a value) and false otherwise */
  public boolean isSetTimestamp() {
    return __isset_bit_vector.get(__TIMESTAMP_ISSET_ID);
  }

  public void setTimestampIsSet(boolean value) {
    __isset_bit_vector.set(__TIMESTAMP_ISSET_ID, value);
  }

  public String getPlatformId() {
    return this.platformId;
  }

  public Registration setPlatformId(String platformId) {
    this.platformId = platformId;
    return this;
  }

  public void unsetPlatformId() {
    this.platformId = null;
  }

  /** Returns true if field platformId is set (has been assigned a value) and false otherwise */
  public boolean isSetPlatformId() {
    return this.platformId != null;
  }

  public void setPlatformIdIsSet(boolean value) {
    if (!value) {
      this.platformId = null;
    }
  }

  public String getSystemVersion() {
    return this.systemVersion;
  }

  public Registration setSystemVersion(String systemVersion) {
    this.systemVersion = systemVersion;
    return this;
  }

  public void unsetSystemVersion() {
    this.systemVersion = null;
  }

  /** Returns true if field systemVersion is set (has been assigned a value) and false otherwise */
  public boolean isSetSystemVersion() {
    return this.systemVersion != null;
  }

  public void setSystemVersionIsSet(boolean value) {
    if (!value) {
      this.systemVersion = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case UU_ID:
      if (value == null) {
        unsetUuId();
      } else {
        setUuId((String)value);
      }
      break;

    case TIMESTAMP:
      if (value == null) {
        unsetTimestamp();
      } else {
        setTimestamp((Double)value);
      }
      break;

    case PLATFORM_ID:
      if (value == null) {
        unsetPlatformId();
      } else {
        setPlatformId((String)value);
      }
      break;

    case SYSTEM_VERSION:
      if (value == null) {
        unsetSystemVersion();
      } else {
        setSystemVersion((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case UU_ID:
      return getUuId();

    case TIMESTAMP:
      return Double.valueOf(getTimestamp());

    case PLATFORM_ID:
      return getPlatformId();

    case SYSTEM_VERSION:
      return getSystemVersion();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case UU_ID:
      return isSetUuId();
    case TIMESTAMP:
      return isSetTimestamp();
    case PLATFORM_ID:
      return isSetPlatformId();
    case SYSTEM_VERSION:
      return isSetSystemVersion();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Registration)
      return this.equals((Registration)that);
    return false;
  }

  public boolean equals(Registration that) {
    if (that == null)
      return false;

    boolean this_present_uuId = true && this.isSetUuId();
    boolean that_present_uuId = true && that.isSetUuId();
    if (this_present_uuId || that_present_uuId) {
      if (!(this_present_uuId && that_present_uuId))
        return false;
      if (!this.uuId.equals(that.uuId))
        return false;
    }

    boolean this_present_timestamp = true && this.isSetTimestamp();
    boolean that_present_timestamp = true && that.isSetTimestamp();
    if (this_present_timestamp || that_present_timestamp) {
      if (!(this_present_timestamp && that_present_timestamp))
        return false;
      if (this.timestamp != that.timestamp)
        return false;
    }

    boolean this_present_platformId = true && this.isSetPlatformId();
    boolean that_present_platformId = true && that.isSetPlatformId();
    if (this_present_platformId || that_present_platformId) {
      if (!(this_present_platformId && that_present_platformId))
        return false;
      if (!this.platformId.equals(that.platformId))
        return false;
    }

    boolean this_present_systemVersion = true && this.isSetSystemVersion();
    boolean that_present_systemVersion = true && that.isSetSystemVersion();
    if (this_present_systemVersion || that_present_systemVersion) {
      if (!(this_present_systemVersion && that_present_systemVersion))
        return false;
      if (!this.systemVersion.equals(that.systemVersion))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(Registration other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    Registration typedOther = (Registration)other;

    lastComparison = Boolean.valueOf(isSetUuId()).compareTo(typedOther.isSetUuId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetUuId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.uuId, typedOther.uuId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetTimestamp()).compareTo(typedOther.isSetTimestamp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTimestamp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.timestamp, typedOther.timestamp);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetPlatformId()).compareTo(typedOther.isSetPlatformId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetPlatformId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.platformId, typedOther.platformId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSystemVersion()).compareTo(typedOther.isSetSystemVersion());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSystemVersion()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.systemVersion, typedOther.systemVersion);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Registration(");
    boolean first = true;

    sb.append("uuId:");
    if (this.uuId == null) {
      sb.append("null");
    } else {
      sb.append(this.uuId);
    }
    first = false;
    if (isSetTimestamp()) {
      if (!first) sb.append(", ");
      sb.append("timestamp:");
      sb.append(this.timestamp);
      first = false;
    }
    if (isSetPlatformId()) {
      if (!first) sb.append(", ");
      sb.append("platformId:");
      if (this.platformId == null) {
        sb.append("null");
      } else {
        sb.append(this.platformId);
      }
      first = false;
    }
    if (isSetSystemVersion()) {
      if (!first) sb.append(", ");
      sb.append("systemVersion:");
      if (this.systemVersion == null) {
        sb.append("null");
      } else {
        sb.append(this.systemVersion);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (uuId == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'uuId' was not present! Struct: " + toString());
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bit_vector = new BitSet(1);
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class RegistrationStandardSchemeFactory implements SchemeFactory {
    public RegistrationStandardScheme getScheme() {
      return new RegistrationStandardScheme();
    }
  }

  private static class RegistrationStandardScheme extends StandardScheme<Registration> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, Registration struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // UU_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.uuId = iprot.readString();
              struct.setUuIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // TIMESTAMP
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.timestamp = iprot.readDouble();
              struct.setTimestampIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // PLATFORM_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.platformId = iprot.readString();
              struct.setPlatformIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // SYSTEM_VERSION
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.systemVersion = iprot.readString();
              struct.setSystemVersionIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, Registration struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.uuId != null) {
        oprot.writeFieldBegin(UU_ID_FIELD_DESC);
        oprot.writeString(struct.uuId);
        oprot.writeFieldEnd();
      }
      if (struct.isSetTimestamp()) {
        oprot.writeFieldBegin(TIMESTAMP_FIELD_DESC);
        oprot.writeDouble(struct.timestamp);
        oprot.writeFieldEnd();
      }
      if (struct.platformId != null) {
        if (struct.isSetPlatformId()) {
          oprot.writeFieldBegin(PLATFORM_ID_FIELD_DESC);
          oprot.writeString(struct.platformId);
          oprot.writeFieldEnd();
        }
      }
      if (struct.systemVersion != null) {
        if (struct.isSetSystemVersion()) {
          oprot.writeFieldBegin(SYSTEM_VERSION_FIELD_DESC);
          oprot.writeString(struct.systemVersion);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class RegistrationTupleSchemeFactory implements SchemeFactory {
    public RegistrationTupleScheme getScheme() {
      return new RegistrationTupleScheme();
    }
  }

  private static class RegistrationTupleScheme extends TupleScheme<Registration> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Registration struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      oprot.writeString(struct.uuId);
      BitSet optionals = new BitSet();
      if (struct.isSetTimestamp()) {
        optionals.set(0);
      }
      if (struct.isSetPlatformId()) {
        optionals.set(1);
      }
      if (struct.isSetSystemVersion()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetTimestamp()) {
        oprot.writeDouble(struct.timestamp);
      }
      if (struct.isSetPlatformId()) {
        oprot.writeString(struct.platformId);
      }
      if (struct.isSetSystemVersion()) {
        oprot.writeString(struct.systemVersion);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Registration struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.uuId = iprot.readString();
      struct.setUuIdIsSet(true);
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.timestamp = iprot.readDouble();
        struct.setTimestampIsSet(true);
      }
      if (incoming.get(1)) {
        struct.platformId = iprot.readString();
        struct.setPlatformIdIsSet(true);
      }
      if (incoming.get(2)) {
        struct.systemVersion = iprot.readString();
        struct.setSystemVersionIsSet(true);
      }
    }
  }

}

