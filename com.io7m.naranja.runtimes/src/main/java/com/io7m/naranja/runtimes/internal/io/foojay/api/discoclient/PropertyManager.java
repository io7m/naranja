/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.naranja.runtimes.internal.io.foojay.api.discoclient;

import java.util.Properties;

import static io.foojay.api.discoclient.util.Constants.API_VERSION_V3;
import static io.foojay.api.discoclient.util.Constants.DISCO_API_BASE_URL;
import static io.foojay.api.discoclient.util.Constants.DISTRIBUTION_JSON_URL;
import static io.foojay.api.discoclient.util.Constants.PROPERTY_KEY_DISCO_URL;
import static io.foojay.api.discoclient.util.Constants.PROPERTY_KEY_DISCO_VERSION;
import static io.foojay.api.discoclient.util.Constants.PROPERTY_KEY_DISTRIBUTION_JSON_URL;

/**
 * A patched PropertyManager that doesn't write hundreds of turds into the
 * filesystem.
 */

// CHECKSTYLE:OFF

public enum PropertyManager
{
  INSTANCE;

  private Properties properties;

  PropertyManager()
  {
    this.properties = new Properties();
    this.createProperties(this.properties);
  }

  public Properties getProperties()
  {
    return this.properties;
  }

  public Object get(final String KEY)
  {
    return this.properties.getOrDefault(KEY, "");
  }

  public void set(
    final String KEY,
    final String VALUE)
  {
    this.properties.setProperty(KEY, VALUE);
  }

  public String getString(final String key)
  {
    return this.properties.getOrDefault(key, "").toString();
  }

  public void setString(
    final String key,
    final String value)
  {
    this.properties.setProperty(key, value);
  }

  public double getDouble(final String key)
  {
    return this.getDouble(key, 0);
  }

  public double getDouble(
    final String key,
    final double defaultValue)
  {
    return Double.parseDouble(this.properties.getOrDefault(
      key,
      Double.toString(defaultValue)).toString());
  }

  public void setDouble(
    final String key,
    final double value)
  {
    this.properties.setProperty(key, Double.toString(value));
  }

  public float getFloat(final String key)
  {
    return this.getFloat(key, 0);
  }

  public float getFloat(
    final String key,
    final float defaultValue)
  {
    return Float.parseFloat(this.properties.getOrDefault(
      key,
      Float.toString(defaultValue)).toString());
  }

  public void setFloat(
    final String key,
    final float value)
  {
    this.properties.setProperty(key, Float.toString(value));
  }

  public int getInt(final String key)
  {
    return this.getInt(key, 0);
  }

  public int getInt(
    final String key,
    final int defaultValue)
  {
    return Integer.parseInt(this.properties.getOrDefault(
      key,
      Integer.toString(defaultValue)).toString());
  }

  public void setInt(
    final String key,
    final int value)
  {
    this.properties.setProperty(key, Integer.toString(value));
  }

  public long getLong(final String key)
  {
    return this.getLong(key, 0);
  }

  public long getLong(
    final String key,
    final long defaultValue)
  {
    return Long.parseLong(this.properties.getOrDefault(
      key,
      Long.toString(defaultValue)).toString());
  }

  public void setLong(
    final String key,
    final long value)
  {
    this.properties.setProperty(key, Long.toString(value));
  }

  public boolean getBoolean(final String key)
  {
    return this.getBoolean(key, false);
  }

  public boolean getBoolean(
    final String key,
    final boolean defaultValue)
  {
    return Boolean.parseBoolean(this.properties.getOrDefault(
      key,
      Boolean.toString(defaultValue)).toString());
  }

  public void setBoolean(
    final String key,
    final boolean value)
  {
    this.properties.setProperty(key, Boolean.toString(value));
  }

  public boolean hasKey(final String key)
  {
    return this.properties.containsKey(key);
  }

  public String getApiVersion()
  {
    return this.properties.getOrDefault(
      PROPERTY_KEY_DISCO_VERSION,
      API_VERSION_V3).toString();
  }

  public String getPackagesPath()
  {
    final var apiVersion = this.getApiVersion();
    return new StringBuilder().append("/disco/v").append(apiVersion).append(
      "/packages").toString();
  }

  public String getEphemeralIdsPath()
  {
    final var apiVersion = this.getApiVersion();
    return new StringBuilder().append("/disco/v").append(apiVersion).append(
      "/ephemeral_ids").toString();
  }

  public String getMajorVersionsPath()
  {
    final var apiVersion = this.getApiVersion();
    return new StringBuilder().append("/disco/v").append(apiVersion).append(
      "/major_versions").toString();
  }

  public String getIdsPath()
  {
    final var apiVersion = this.getApiVersion();
    return new StringBuilder().append("/disco/v").append(apiVersion).append(
      "/ids").toString();
  }

  public String getDistributionsPath()
  {
    final var apiVersion = this.getApiVersion();
    return new StringBuilder().append("/disco/v").append(apiVersion).append(
      "/distributions").toString();
  }


  // ******************** Properties ****************************************
  public void storeProperties()
  {

  }

  private void createProperties(
    final Properties properties)
  {
    properties.put(PROPERTY_KEY_DISCO_URL, DISCO_API_BASE_URL);
    properties.put(PROPERTY_KEY_DISCO_VERSION, API_VERSION_V3);
    properties.put(PROPERTY_KEY_DISTRIBUTION_JSON_URL, DISTRIBUTION_JSON_URL);
  }
}

