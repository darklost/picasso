package com.squareup.picasso;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class RequestData {
  public final Uri uri;
  public final int resourceId;
  public final List<Transformation> transformations;
  public final int targetWidth;
  public final int targetHeight;
  public final boolean centerCrop;
  public final boolean centerInside;
  public final float rotationDegrees;
  public final float rotationPivotX;
  public final float rotationPivotY;
  public final boolean hasRotationPivot;

  private RequestData(Uri uri, int resourceId, List<Transformation> transformations,
      int targetWidth, int targetHeight, boolean centerCrop, boolean centerInside,
      float rotationDegrees, float rotationPivotX, float rotationPivotY, boolean hasRotationPivot) {
    this.uri = uri;
    this.resourceId = resourceId;
    if (transformations == null) {
      this.transformations = null;
    } else {
      this.transformations = unmodifiableList(new ArrayList<Transformation>(transformations));
    }
    this.targetWidth = targetWidth;
    this.targetHeight = targetHeight;
    this.centerCrop = centerCrop;
    this.centerInside = centerInside;
    this.rotationDegrees = rotationDegrees;
    this.rotationPivotX = rotationPivotX;
    this.rotationPivotY = rotationPivotY;
    this.hasRotationPivot = hasRotationPivot;
  }

  @Override public String toString() {
    if (uri != null) {
      return uri.getPath();
    }
    return Integer.toHexString(resourceId);
  }

  boolean hasSize() {
    return targetWidth != 0;
  }

  boolean needsTransformation() {
    return needsMatrixTransform() || hasCustomTransformations();
  }

  boolean needsMatrixTransform() {
    return targetWidth != 0 || rotationDegrees != 0;
  }

  boolean hasCustomTransformations() {
    return transformations != null;
  }

  public static class Builder {
    private final Uri uri;
    private final int resourceId;
    private int targetWidth;
    private int targetHeight;
    private boolean centerCrop;
    private boolean centerInside;
    private float rotationDegrees;
    private float rotationPivotX;
    private float rotationPivotY;
    private boolean hasRotation;
    private boolean hasRotationPivot;
    private List<Transformation> transformations;

    public Builder(Uri uri) {
      if (uri == null) {
        throw new IllegalArgumentException(); // TODO
      }
      this.uri = uri;
      this.resourceId = 0;
    }

    public Builder(int resourceId) {
      if (resourceId == 0) {
        throw new IllegalArgumentException(); // TODO
      }
      this.uri = null;
      this.resourceId = resourceId;
    }

    Builder(Uri uri, int resourceId) {
      this.uri = uri;
      this.resourceId = resourceId;
    }

    boolean hasImage() {
      return uri != null || resourceId != 0;
    }

    boolean hasSize() {
      return targetWidth != 0;
    }

    public Builder resize(int targetWidth, int targetHeight) {
      if (targetWidth <= 0) {
        throw new IllegalArgumentException("Width must be positive number.");
      }
      if (targetHeight <= 0) {
        throw new IllegalArgumentException("Height must be positive number.");
      }
      if (hasSize()) {
        throw new IllegalStateException("Resize may only be called once.");
      }

      this.targetWidth = targetWidth;
      this.targetHeight = targetHeight;
      return this;
    }

    public Builder centerCrop() {
      if (targetWidth == 0) {
        throw new IllegalStateException("Center crop can only be used after calling resize.");
      }
      if (centerInside) {
        throw new IllegalStateException("Center crop can not be used after calling centerInside");
      }

      centerCrop = true;
      return this;
    }

    public Builder centerInside() {
      if (targetWidth == 0) {
        throw new IllegalStateException("Center crop can only be used after calling resize.");
      }
      if (centerCrop) {
        throw new IllegalStateException("Center crop can not be used after calling centerInside");
      }

      centerInside = true;
      return this;
    }

    public Builder rotate(float degrees) {
      if (hasRotation) {
        throw new IllegalStateException("Rotate may only be called once.");
      }
      rotationDegrees = degrees;
      hasRotation = true;
      return this;
    }

    public Builder rotate(float degrees, float pivotX, float pivotY) {
      if (hasRotation) {
        throw new IllegalStateException("Rotate may only be called once.");
      }
      rotationDegrees = degrees;
      rotationPivotX = pivotX;
      rotationPivotY = pivotY;
      hasRotation = true;
      hasRotationPivot = true;
      return this;
    }

    public Builder transform(Transformation transformation) {
      if (transformation == null) {
        throw new IllegalArgumentException("Transformation must not be null.");
      }
      if (transformations == null) {
        transformations = new ArrayList<Transformation>(2);
      }
      transformations.add(transformation);
      return this;
    }

    public RequestData build() {
      return new RequestData(uri, resourceId, transformations, targetWidth, targetHeight,
          centerCrop, centerInside, rotationDegrees, rotationPivotX, rotationPivotY,
          hasRotationPivot);
    }
  }
}
