/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import java.io.IOException;
import org.jetbrains.annotations.TestOnly;

import static com.squareup.picasso.BitmapHunter.forRequest;
import static com.squareup.picasso.Picasso.LoadedFrom.MEMORY;
import static com.squareup.picasso.Utils.checkNotMain;
import static com.squareup.picasso.Utils.createKey;

/** Fluent API for building an image download request. */
@SuppressWarnings("UnusedDeclaration") // Public API.
public class RequestBuilder {
  private final Picasso picasso;
  private final RequestData.Builder data;

  private boolean skipMemoryCache;
  private boolean noFade;
  private boolean deferred;
  private int placeholderResId;
  private Drawable placeholderDrawable;
  private int errorResId;
  private Drawable errorDrawable;

  RequestBuilder(Picasso picasso, Uri uri, int resourceId) {
    if (picasso.shutdown) {
      throw new IllegalStateException(
          "Picasso instance already shut down. Cannot submit new requests.");
    }
    this.picasso = picasso;
    this.data = new RequestData.Builder(uri, resourceId);
  }

  @TestOnly RequestBuilder() {
    this.picasso = null;
    this.data = new RequestData.Builder(null, 0);
  }

  /**
   * A placeholder drawable to be used while the image is being loaded. If the requested image is
   * not immediately available in the memory cache then this resource will be set on the target
   * {@link ImageView}.
   */
  public RequestBuilder placeholder(int placeholderResId) {
    if (placeholderResId == 0) {
      throw new IllegalArgumentException("Placeholder image resource invalid.");
    }
    if (placeholderDrawable != null) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    this.placeholderResId = placeholderResId;
    return this;
  }

  /**
   * A placeholder drawable to be used while the image is being loaded. If the requested image is
   * not immediately available in the memory cache then this resource will be set on the target
   * {@link ImageView}.
   * <p>
   * If you are not using a placeholder image but want to clear an existing image (such as when
   * used in an {@link android.widget.Adapter adapter}), pass in {@code null}.
   */
  public RequestBuilder placeholder(Drawable placeholderDrawable) {
    if (placeholderResId != 0) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    this.placeholderDrawable = placeholderDrawable;
    return this;
  }

  /** An error drawable to be used if the request image could not be loaded. */
  public RequestBuilder error(int errorResId) {
    if (errorResId == 0) {
      throw new IllegalArgumentException("Error image resource invalid.");
    }
    if (errorDrawable != null) {
      throw new IllegalStateException("Error image already set.");
    }
    this.errorResId = errorResId;
    return this;
  }

  /** An error drawable to be used if the request image could not be loaded. */
  public RequestBuilder error(Drawable errorDrawable) {
    if (errorDrawable == null) {
      throw new IllegalArgumentException("Error image may not be null.");
    }
    if (errorResId != 0) {
      throw new IllegalStateException("Error image already set.");
    }
    this.errorDrawable = errorDrawable;
    return this;
  }

  /**
   * Attempt to resize the image to fit exactly into the target {@link ImageView}'s bounds. This
   * will result in delayed execution of the request until the {@link ImageView} has been measured.
   * <p/>
   * <em>Note:</em> This method works only when your target is an {@link ImageView).
   */
  public RequestBuilder fit() {
    if (!data.hasSize()) {
      throw new IllegalStateException("Fit cannot be used with resize.");
    }

    deferred = true;
    return this;
  }

  /** Resize the image to the specified dimension size. */
  public RequestBuilder resizeDimen(int targetWidthResId, int targetHeightResId) {
    Resources resources = picasso.context.getResources();
    int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
    int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
    return resize(targetWidth, targetHeight);
  }

  /** Resize the image to the specified size in pixels. */
  public RequestBuilder resize(int targetWidth, int targetHeight) {
    if (deferred) {
      throw new IllegalStateException("Resize cannot be used with fit.");
    }
    data.resize(targetWidth, targetHeight);
    return this;
  }

  /**
   * Crops an image inside of the bounds specified by {@link #resize(int, int)} rather than
   * distorting the aspect ratio. This cropping technique scales the image so that it fills the
   * requested bounds and then crops the extra.
   */
  public RequestBuilder centerCrop() {
    data.centerCrop();
    return this;
  }

  /**
   * Centers an image inside of the bounds specified by {@link #resize(int, int)}. This scales
   * the image so that both dimensions are equal to or less than the requested bounds.
   */
  public RequestBuilder centerInside() {
    data.centerInside();
    return this;
  }

  /** Rotate the image by the specified degrees. */
  public RequestBuilder rotate(float degrees) {
    data.rotate(degrees);
    return this;
  }

  /** Rotate the image by the specified degrees around a pivot point. */
  public RequestBuilder rotate(float degrees, float pivotX, float pivotY) {
    data.rotate(degrees, pivotX, pivotY);
    return this;
  }

  /**
   * Add a custom transformation to be applied to the image.
   * <p/>
   * Custom transformations will always be run after the built-in transformations.
   */
  // TODO show example of calling resize after a transform in the javadoc
  public RequestBuilder transform(Transformation transformation) {
    data.transform(transformation);
    return this;
  }

  /**
   * Indicate that this request should not use the memory cache for attempting to load or save the
   * image. This can be useful when you know an image will only ever be used once (e.g., loading
   * an image from the filesystem and uploading to a remote server).
   */
  public RequestBuilder skipMemoryCache() {
    skipMemoryCache = true;
    return this;
  }

  /** Disable brief fade in of images loaded from the disk cache or network. */
  public RequestBuilder noFade() {
    noFade = true;
    return this;
  }

  /** Synchronously fulfill this request. Must not be called from the main thread. */
  public Bitmap get() throws IOException {
    checkNotMain();
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with get.");
    }
    if (!data.hasImage()) {
      return null;
    }

    RequestData finalData = picasso.requestTransformer.transformRequest(data.build());

    Request request = new GetRequest(picasso, finalData, skipMemoryCache);
    return forRequest(picasso.context, picasso, picasso.dispatcher, picasso.cache, request,
        picasso.dispatcher.downloader, Utils.isAirplaneModeOn(picasso.context)).hunt();
  }

  /**
   * Asynchronously fulfills the request without a {@link ImageView} or {@link Target}. This is
   * useful when you want to warm up the cache with an image.
   */
  public void fetch() {
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with fetch.");
    }
    if (data.hasImage()) {
      RequestData finalData = picasso.requestTransformer.transformRequest(data.build());
      Request request = new FetchRequest(picasso, finalData, skipMemoryCache);
      picasso.enqueueAndSubmit(request);
    }
  }

  /**
   * Asynchronously fulfills the request into the specified {@link Target}. In most cases, you
   * should use this when you are dealing with a custom {@link android.view.View} which should
   * implement the {@link Target} interface.
   * <p/>
   * <em>Note:</em> This method keeps a weak reference to the {@link Target} instance and will be
   * garbage collected if you do not keep a strong reference to it.
   */
  public void into(Target target) {
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null.");
    }
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with a Target.");
    }
    if (!data.hasImage()) {
      picasso.cancelRequest(target);
      return;
    }

    RequestData finalData = picasso.requestTransformer.transformRequest(data.build());
    String requestKey = createKey(finalData);

    if (!skipMemoryCache) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck(requestKey);
      if (bitmap != null) {
        picasso.cancelRequest(target);
        target.onBitmapLoaded(bitmap, MEMORY);
        return;
      }
    }

    Request request = new TargetRequest(picasso, target, finalData, skipMemoryCache, requestKey);
    picasso.enqueueAndSubmit(request);
  }

  /**
   * Asynchronously fulfills the request into the specified {@link ImageView}.
   * <p/>
   * <em>Note:</em> This method keeps a weak reference to the {@link ImageView} instance and will
   * automatically support object recycling.
   */
  public void into(ImageView target) {
    into(target, null);
  }

  /**
   * Asynchronously fulfills the request into the specified {@link ImageView} and invokes the
   * target {@link Callback} if it's not {@code null}.
   * <p/>
   * <em>Note:</em> The {@link Callback} param is a strong reference and will prevent your
   * {@link android.app.Activity} or {@link android.app.Fragment} from being garbage collected. If
   * you use this method, it is <b>strongly</b> recommended you invoke an adjacent
   * {@link Picasso#cancelRequest(android.widget.ImageView)} call to prevent temporary leaking.
   */
  public void into(ImageView target, Callback callback) {
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null.");
    }

    if (!data.hasImage()) {
      picasso.cancelRequest(target);
      PicassoDrawable.setPlaceholder(target, placeholderResId, placeholderDrawable);
      return;
    }
    if (deferred) {
      // TODO!!!!!!!
      return;
    }

    RequestData finalData = picasso.requestTransformer.transformRequest(data.build());
    String requestKey = createKey(finalData);

    if (!skipMemoryCache) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck(requestKey);
      if (bitmap != null) {
        picasso.cancelRequest(target);
        PicassoDrawable.setBitmap(target, picasso.context, bitmap, MEMORY, noFade,
            picasso.debugging);
        if (callback != null) {
          callback.onSuccess();
        }
        return;
      }
    }

    PicassoDrawable.setPlaceholder(target, placeholderResId, placeholderDrawable);

    Request request = new ImageViewRequest(picasso, target, finalData,
        skipMemoryCache, noFade, errorResId, errorDrawable, requestKey, callback);

    picasso.enqueueAndSubmit(request);
  }
}
