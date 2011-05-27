/*
 * Copyright (C) 2011 The Android Open Source Project
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


package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.NativeProgram;
import android.filterfw.core.NativeFrame;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;

import android.util.Log;

public class ToRGBFilter extends Filter {

    private int mInputBPP;
    private Program mProgram;
    private MutableFrameFormat mOutputFormat;

    public ToRGBFilter(String name) {
        super(name);
    }

    public String[] getInputNames() {
        return new String[] { "image" };
    }

    public String[] getOutputNames() {
        return new String[] { "image" };
    }

    public boolean acceptsInputFormat(int index, FrameFormat format) {
        if (format.isBinaryDataType()) {
            mInputBPP = format.getBytesPerSample();
            mOutputFormat = format.mutableCopy();
            mOutputFormat.setBytesPerSample(3);
            return true;
        }
        return false;
    }

    public FrameFormat getOutputFormat(int index) {
        return mOutputFormat;
    }

    public void prepare(FilterContext environment) {
        switch (mOutputFormat.getTarget()) {
            case FrameFormat.TARGET_NATIVE:
                switch (mInputBPP) {
                    case 1:
                        mProgram = new NativeProgram("filterpack_imageproc", "gray_to_rgb");
                        break;
                    case 4:
                        mProgram = new NativeProgram("filterpack_imageproc", "rgba_to_rgb");
                        break;
                    default:
                        throw new RuntimeException("Unsupported BytesPerPixel: " + mInputBPP + "!");
                }
                break;

            case FrameFormat.TARGET_GPU:
                throw new RuntimeException("GPU ToRGB not implemented yet!");

            default:
                throw new RuntimeException("ToRGBFilter could not create suitable program!");
        }
    }

    public int process(FilterContext env) {
        // Get input frame
        Frame input = pullInput(0);

        // Create output frame
        MutableFrameFormat outputFormat = input.getFormat().mutableCopy();
        outputFormat.setBytesPerSample(3);
        Frame output = env.getFrameManager().newFrame(outputFormat);

        // Process
        mProgram.process(input, output);

        // Push output
        putOutput(0, output);

        // Release pushed frame
        output.release();

        // Wait for next input and free output
        return Filter.STATUS_WAIT_FOR_ALL_INPUTS |
                Filter.STATUS_WAIT_FOR_FREE_OUTPUTS;
    }


}
