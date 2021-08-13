/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm.flicker;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertWithMessage;

import androidx.annotation.Nullable;

import com.android.server.wm.flicker.Assertions.Result;
import com.android.server.wm.flicker.TransitionRunner.TransitionResult;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Truth subject for {@link WindowManagerTrace} objects. */
public class WmTraceSubject extends Subject {
    // Boiler-plate Subject.Factory for WmTraceSubject
    private static final Subject.Factory<WmTraceSubject, WindowManagerTrace> FACTORY =
            WmTraceSubject::new;

    private AssertionsChecker<WindowManagerTrace.Entry> mChecker = new AssertionsChecker<>();
    private WindowManagerTrace mTrace;
    private boolean mNewAssertion = true;

    private void addAssertion(
            Assertions.TraceAssertion<WindowManagerTrace.Entry> assertion, String name) {
        if (mNewAssertion) {
            mChecker.add(assertion, name);
        } else {
            mChecker.append(assertion, name);
        }
    }

    private WmTraceSubject(FailureMetadata fm, @Nullable WindowManagerTrace subject) {
        super(fm, subject);
        mTrace = subject;
    }

    // User-defined entry point
    public static WmTraceSubject assertThat(@Nullable WindowManagerTrace entry) {
        return assertAbout(FACTORY).that(entry);
    }

    // User-defined entry point
    public static WmTraceSubject assertThat(@Nullable TransitionResult result) {
        WindowManagerTrace entries =
                WindowManagerTrace.parseFrom(
                        result.getWindowManagerTrace(),
                        result.getWindowManagerTracePath(),
                        result.getWindowManagerTraceChecksum());
        return assertWithMessage(result.toString()).about(FACTORY).that(entries);
    }

    // Static method for getting the subject factory (for use with assertAbout())
    public static Subject.Factory<WmTraceSubject, WindowManagerTrace> entries() {
        return FACTORY;
    }

    public void forAllEntries() {
        test();
    }

    public void forRange(long startTime, long endTime) {
        mChecker.filterByRange(startTime, endTime);
        test();
    }

    public WmTraceSubject then() {
        mNewAssertion = true;
        mChecker.checkChangingAssertions();
        return this;
    }

    public WmTraceSubject and() {
        mNewAssertion = false;
        mChecker.checkChangingAssertions();
        return this;
    }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    public WmTraceSubject skipUntilFirstAssertion() {
        mChecker.skipUntilFirstAssertion();
        return this;
    }

    public void inTheBeginning() {
        if (mTrace.getEntries().isEmpty()) {
            failWithActual("No entries found.", mTrace);
        }
        mChecker.checkFirstEntry();
        test();
    }

    public void atTheEnd() {
        if (mTrace.getEntries().isEmpty()) {
            failWithActual("No entries found.", mTrace);
        }
        mChecker.checkLastEntry();
        test();
    }

    private void test() {
        List<Result> failures = mChecker.test(mTrace.getEntries());
        if (!failures.isEmpty()) {
            Optional<Path> failureTracePath = mTrace.getSource();
            String failureLogs =
                    failures.stream().map(Result::toString).collect(Collectors.joining("\n"));
            String tracePath = "";
            if (failureTracePath.isPresent()) {
                tracePath =
                        "\nWindowManager Trace can be found in: "
                                + failureTracePath.get().toAbsolutePath()
                                + "\nChecksum: "
                                + mTrace.getSourceChecksum()
                                + "\n";
            }
            failWithActual(tracePath + failureLogs, mTrace);
        }
    }

    public WmTraceSubject showsAboveAppWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isAboveAppWindowVisible(partialWindowTitle),
                "showsAboveAppWindow(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject hidesAboveAppWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isAboveAppWindowVisible(partialWindowTitle).negate(),
                "hidesAboveAppWindow" + "(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject showsBelowAppWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isBelowAppWindowVisible(partialWindowTitle),
                "showsBelowAppWindow(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject hidesBelowAppWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isBelowAppWindowVisible(partialWindowTitle).negate(),
                "hidesBelowAppWindow" + "(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject showsImeWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isImeWindowVisible(partialWindowTitle),
                "showsBelowAppWindow(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject hidesImeWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isImeWindowVisible(partialWindowTitle).negate(),
                "hidesImeWindow" + "(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject showsAppWindowOnTop(String partialWindowTitle) {
        addAssertion(
                entry -> {
                    Result result = entry.isAppWindowVisible(partialWindowTitle);
                    if (result.passed()) {
                        result = entry.isVisibleAppWindowOnTop(partialWindowTitle);
                    }
                    return result;
                },
                "showsAppWindowOnTop(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject hidesAppWindowOnTop(String partialWindowTitle) {
        addAssertion(
                entry -> {
                    Result result = entry.isAppWindowVisible(partialWindowTitle).negate();
                    if (result.failed()) {
                        result = entry.isVisibleAppWindowOnTop(partialWindowTitle).negate();
                    }
                    return result;
                },
                "hidesAppWindowOnTop(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject showsAppWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isAppWindowVisible(partialWindowTitle),
                "showsAppWindow(" + partialWindowTitle + ")");
        return this;
    }

    public WmTraceSubject hidesAppWindow(String partialWindowTitle) {
        addAssertion(
                entry -> entry.isAppWindowVisible(partialWindowTitle).negate(),
                "hidesAppWindow(" + partialWindowTitle + ")");
        return this;
    }
}