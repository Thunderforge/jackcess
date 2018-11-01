/*
Copyright (c) 2018 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.healthmarketscience.jackcess.impl.expr;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

import com.healthmarketscience.jackcess.expr.LocaleContext;
import com.healthmarketscience.jackcess.expr.EvalException;
import com.healthmarketscience.jackcess.expr.Value;
import com.healthmarketscience.jackcess.impl.ColumnImpl;

/**
 *
 * @author James Ahlborn
 */
public class ValueSupport
{
  public static final Value NULL_VAL = new BaseValue() {
    @Override public boolean isNull() {
      return true;
    }
    public Type getType() {
      return Type.NULL;
    }
    public Object get() {
      return null;
    }
  };
  // access seems to like -1 for true and 0 for false (boolean values are
  // basically an illusion)
  public static final Value TRUE_VAL = new LongValue(-1);
  public static final Value FALSE_VAL = new LongValue(0);
  public static final Value EMPTY_STR_VAL = new StringValue("");
  public static final Value ZERO_VAL = FALSE_VAL;
  public static final Value NEG_ONE_VAL = TRUE_VAL;
  public static final Value ONE_VAL = new LongValue(1);

  private ValueSupport() {}

  public static Value toValue(boolean b) {
    return (b ? TRUE_VAL : FALSE_VAL);
  }

  public static Value toValue(String s) {
    return new StringValue(s);
  }

  public static Value toValue(int i) {
    return new LongValue(i);
  }

  public static Value toValue(Integer i) {
    return new LongValue(i);
  }

  public static Value toValue(float f) {
    return new DoubleValue((double)f);
  }

  public static Value toValue(double s) {
    return new DoubleValue(s);
  }

  public static Value toValue(Double s) {
    return new DoubleValue(s);
  }

  public static Value toValue(BigDecimal s) {
    return new BigDecimalValue(normalize(s));
  }

  public static Value toDateValue(LocaleContext ctx, Value.Type type, double dd)
  {
    return toValue(type, new Date(
                       ColumnImpl.fromDateDouble(dd, ctx.getCalendar())));
  }

  public static Value toValue(Value.Type type, Date d) {
    return new DateTimeValue(type, d);
  }

  public static DateFormat getDateFormatForType(LocaleContext ctx, Value.Type type) {
    String fmtStr = null;
    switch(type) {
    case DATE:
      fmtStr = ctx.getTemporalConfig().getDefaultDateFormat();
      break;
    case TIME:
      fmtStr = ctx.getTemporalConfig().getDefaultTimeFormat();
      break;
    case DATE_TIME:
      fmtStr = ctx.getTemporalConfig().getDefaultDateTimeFormat();
      break;
    default:
      throw new EvalException("Unexpected date/time type " + type);
    }
    return ctx.createDateFormat(fmtStr);
  }

  /**
   * Converts the given BigDecimal to the minimal scale >= 0;
   */
  static BigDecimal normalize(BigDecimal bd) {
    if(bd.scale() == 0) {
      return bd;
    }
    // handle a bug in the jdk which doesn't strip zero values
    if(bd.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    bd = bd.stripTrailingZeros();
    if(bd.scale() < 0) {
      bd = bd.setScale(0);
    }
    return bd;
  }
}
