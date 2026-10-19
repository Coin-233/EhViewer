#![cfg(feature = "android-26")]

use super::android::use_bitmap_content;
use super::jvm::jni_throwing;
use crate::img::copy_region::{CopyRegion, cast_image_ptr};
use crate::img::webp::{decode_next_frame, get_image_info};
use image::{GenericImage, Rgba};
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jboolean, jint, jlong, jobject};
use jni_fn::jni_fn;
use libwebp_sys::WebPAnimDecoder;
use ndk::hardware_buffer::{HardwareBuffer, HardwareBufferUsage};

#[jni_fn("com.hippo.ehviewer.image.ImageKt")]
pub fn copyBitmapToAHB(mut env: JNIEnv, _: JClass, bm: jobject, ahb: jobject, x: jint, y: jint) {
    let ahb = unsafe { HardwareBuffer::from_jni(env.get_raw(), ahb) };
    jni_throwing(&mut env, |env| {
        let desc = ahb.describe();
        let (w, h, stride) = (desc.width, desc.height, desc.stride);
        let ptr = ahb.lock(HardwareBufferUsage::CPU_WRITE_RARELY, None, None)?;
        let s = CopyRegion { ptr: ptr as *mut !, target_dim: (stride, h), src_rect: (x as u32, y as u32, w, h) };
        let result = use_bitmap_content(env, bm, s);
        ahb.unlock()?;
        result
    })
}

#[jni_fn("com.hippo.ehviewer.coil.AnimatedWebPDrawableKt")]
pub fn nativeDecodeNextFrameZeroCopy(mut env: JNIEnv, _: JClass, decoder: jlong, reset: jboolean, ahb: jobject) -> jint {
    let ahb = unsafe { HardwareBuffer::from_jni(env.get_raw(), ahb) };
    jni_throwing(&mut env, |_env| {
        let dec = decoder as *mut WebPAnimDecoder;
        let (buf, timestamp) = unsafe { decode_next_frame(dec, reset != 0) };
        if !buf.is_null() {
            let desc = ahb.describe();
            let (_, h, stride) = (desc.width, desc.height, desc.stride);
            let ptr = ahb.lock(HardwareBufferUsage::CPU_WRITE_OFTEN, None, None)? as *mut u8;
            let mut dst = cast_image_ptr::<Rgba<u8>>(ptr as *mut !, stride, h);
            let info = unsafe { get_image_info(dec) };
            let (src_w, src_h) = (info.canvas_width, info.canvas_height);
            let src = cast_image_ptr::<Rgba<u8>>(buf as *mut !, src_w, src_h);
            let ret = dst.copy_from(&src, 0, 0);
            ahb.unlock()?;
            ret?
        }
        Ok(timestamp)
    })
}
