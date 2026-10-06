package com.example.foroom.Helper

import android.app.Instrumentation
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ViewPagerActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.Iterables
import org.hamcrest.*
import org.hamcrest.Matchers.anyOf
import java.util.concurrent.TimeoutException
import java.util.function.Predicate


/**
 * Wait for view to be visible
 */
fun ViewInteraction.waitUntilVisible(timeoutSec: Long): ViewInteraction {
    val startTime = System.currentTimeMillis()
    val endTime = startTime + timeoutSec * 1000

    do {
        try {
            check(matches(isDisplayed()))
            return this
        } catch (_: Exception) {
            Thread.sleep(50)
        }
    } while (System.currentTimeMillis() < endTime)

    throw TimeoutException()
}

/**
 * Input Text on View
 */
fun ViewInteraction.input(inputText: String) {
    waitUntilVisible(3)
    tap()
    perform(replaceText(inputText), closeSoftKeyboard())
}

/**
 * Type Text on View
 */
fun Matcher<View>.typeText(text: String, timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(TypeTextAction(text))
}

/**
 * Input Text on view
 */
fun Matcher<View>.waitForViewVisible(timeOutIfNotExitInSec: Int) {
    waitForView(first(this), timeOutIfNotExitInSec * 1000)
}

fun waitForView(first: Matcher<View>, i: Int) {

}

fun Matcher<View>.waitUntilInvisible(sec: Int): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "wait for a specific view with id <$this> is hidden during $sec sec."
        }

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()
            val startTime = System.currentTimeMillis()
            val endTime = startTime + sec * 1000

            do {
                for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                    // found view with required ID
                    if (matches(child) && !child.isShown) {
                        return
                    }
                }
                uiController.loopMainThreadForAtLeast(50)
            } while (System.currentTimeMillis() < endTime)
            throw PerformException.Builder()
                .withActionDescription(this.description)
                .withViewDescription(HumanReadables.describe(view))
                .withCause(TimeoutException())
                .build()
        }
    }
}

/**
 * Get Text from view with ViewMatcher
 */
fun Matcher<View>.getText(timeOutIfNotExitInSec: Int = 3): String {
    var text = String()
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(TextView::class.java)
        }

        override fun getDescription(): String {
            return "Text of the view"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as TextView
            text = tv.text.toString()
        }
    })

    return text
}

/**
 * Check if checkbox is checked
 */
fun Matcher<View>.isChecked(timeOutIfNotExitInSec: Int = 3): Boolean {
    var isChecked = false
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(RadioButton::class.java)
        }

        override fun getDescription(): String {
            return "State of the checkbox"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as RadioButton
            isChecked = tv.isChecked
        }
    })

    return isChecked
}

/**
 * Returns toolbar text
 */
fun Matcher<View>.getToolbarText(timeOutIfNotExitInSec: Int = 3): String {
    var titleText = String()
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(Toolbar::class.java)
        }

        override fun getDescription(): String {
            return "Get toolbar text"
        }

        override fun perform(UiController: UiController, view: View) {
            val tv = view as Toolbar
            titleText = tv.title.toString()
        }
    })

    return titleText
}


/**
 * Returns the transformation method value to check whether edit text is visible or not
 * If text is not visible it will return PasswordTransformationMethod
 */
fun Matcher<View>.editTextIsVisible(timeOutIfNotExitInSec: Int = 3): Boolean {
    var isVisible = false
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(EditText::class.java)
        }

        override fun getDescription(): String {
            return "Get transformation method value"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as EditText
            // if input field has eye button pressing it shows text and transformation method becomes null
            if (tv.transformationMethod == null) {
                isVisible = true
                // if transformation method doesn't contain password text is visible (could be changed by else only)
            } else if (!tv.transformationMethod.toString().contains("Password") && !tv.transformationMethod.toString().contains("password")) {
                isVisible = true
            }
        }
    })
    return isVisible
}

/**
 * Returns text backgroundColor
 */
fun Matcher<View>.getBackgroundColor(timeOutIfNotExitInSec: Int = 3): Int {
    var titleText = 0
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(View::class.java)
        }

        override fun getDescription(): String {
            return "Get background color"
        }

        override fun perform(UiController: UiController, view: View) {
            titleText = (view.background as ColorDrawable).color
        }
    })

    return titleText
}

/**
 * Returns text color
 */
fun Matcher<View>.getTextColor(timeOutIfNotExitInSec: Int = 3): Int {
    var titleText = 0
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(TextView::class.java)
        }

        override fun getDescription(): String {
            return "Get text color"
        }

        override fun perform(UiController: UiController, view: View) {
            val tv = view as TextView
            titleText = tv.currentTextColor
        }
    })

    return titleText
}

/**
 * Checks whether editText is masked (for example password) or not
 */
fun Matcher<View>.isMasked(timeOutIfNotExitInSec: Int = 3): Boolean {
    var isPassword = false
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(EditText::class.java)
        }

        override fun getDescription(): String {
            return "Check if edit text is masked"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as EditText
            // 129 is the bitwise operation result when you set inputType to password, 18 is for numberPassword type
            if (tv.inputType == 129 || tv.inputType == 18)
                isPassword = true
        }
    })

    return isPassword
}

/**
 * get hint from view
 */
fun Matcher<View>.getHint(timeOutIfNotExitInSec: Int = 3): String {
    var hint = String()
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(EditText::class.java)
        }

        override fun getDescription(): String {
            return "Hint of the view"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as EditText
            hint = tv.hint.toString()
        }
    })

    return hint
}

/**
 * Get Text from view
 */
fun ViewInteraction.getText(): String {
    var text = String()
    waitUntilVisible(3)
    perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(TextView::class.java)
        }

        override fun getDescription(): String {
            return "Text of the view"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as TextView
            text = tv.text.toString()
        }
    })

    return text
}


/**
 *  Click on view with viewMatcher by scroll
 */
fun Matcher<View>.scrollAndClick(timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(ViewActions.scrollTo(), ViewActions.click())
}

/**
 *  Click on view with viewMatcher by scroll
 */
fun Matcher<View>.scrollToView(timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(ViewActions.scrollTo())
}

fun Matcher<View>.swipeLeft(timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(ViewActions.swipeLeft())
}

fun Matcher<View>.scrollRight(timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(ViewActions.swipeRight())
}

fun Matcher<View>.scrollLeft(timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(ViewPagerActions.scrollLeft())
}


/**
 *  Click on view with viewMatcher
 */
fun Matcher<View>.tap(timeOutIfNotExitInSec: Int = 3, byPosition: Int = 0) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(withIndex(this, byPosition)).perform(ViewActions.click())
}


/**
 * Click on view
 */
fun ViewInteraction.tap() {
    waitUntilVisible(3).perform(ViewActions.click())
}

/**
 * Click on view to be visible in time
 */
fun ViewInteraction.tap(timeOutIfNotExitInSec: Long) {
    waitUntilVisible(timeOutIfNotExitInSec).perform(ViewActions.click())
}

/**
 *  wait to view for position
 */
fun Matcher<View>.wait(timeOutIfNotExitInSec: Int = 3, forPosition: Int = 0): ViewInteraction {
    waitForView(withIndex(this, forPosition), timeOutIfNotExitInSec * 1000)
    return onView(withIndex(this, forPosition))
}

fun <T> first(matcher: Matcher<T>): Matcher<T> {
    return object : BaseMatcher<T>() {
        var isFirst = true
        override fun matches(item: Any?): Boolean {
            if (isFirst && matcher.matches(item)) {
                isFirst = false
                return true
            }
            return false
        }

        override fun describeTo(description: Description) {
            description.appendText("should return first matching item")
        }
    }
}

/**
 * Get Height of LinearLayout
 */
fun Matcher<View>.getHeightOfView(timeOutIfNotExitInSec: Int = 3): Int {
    var height = 0
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(first(this)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(LinearLayout::class.java)
        }

        override fun getDescription(): String {
            return "Text of the view"
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as LinearLayout

            height = tv.height
        }
    })

    return height
}

fun <T> byViewPosition(matcher: Matcher<T>, itemPosition: Int): Matcher<T> {
    return object : BaseMatcher<T>() {

        var xPosition = 0
        override fun matches(item: Any?): Boolean {
            if (xPosition == itemPosition && matcher.matches(item)) {
                return true
            } else xPosition++
            return false
        }

        override fun describeTo(description: Description) {
            description.appendText("should return first matching item")
        }
    }
}

fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        var currentIndex = 0
        override fun describeTo(description: Description) {
            description.appendText("with index: ")
            description.appendValue(index)
            matcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            return matcher.matches(view) && currentIndex++ == index
        }
    }
}

fun swipeLeftSlowly(): ViewAction {
    return GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER_RIGHT, GeneralLocation.CENTER_LEFT, Press.FINGER)
}

fun swipeLeftFast(): ViewAction {
    return GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT, GeneralLocation.CENTER_LEFT, Press.FINGER)
}

fun swipeUpSlowly(): ViewAction {
    return GeneralSwipeAction(Swipe.SLOW, GeneralLocation.BOTTOM_CENTER, GeneralLocation.TOP_CENTER, Press.FINGER)
}

// This swipes all the way to the bottom of the screen
fun swipeToBottom() {
    swiper(1000, 100, 0)
}

// This scrolls down one page at a time
fun scrollSlowlyDown() {
    swiper(775, 100, 100)
}

// This swipes to the top
fun swipeToTop() {
    swiper(100, 1000, 0)
}

// This scrolls up one page at a time
fun scrollSlowlyUp() {
    swiper(100, 775, 100)
}

fun swiper(start: Int, end: Int, delay: Int) {
    val inst = getInstrumentation()
    val x = inst.targetContext.resources.displayMetrics.widthPixels / 2f
    val downTime = SystemClock.uptimeMillis()
    val duration = delay.coerceAtLeast(200)
    fun send(action: Int, y: Float) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        try { inst.sendPointerSync(event) } finally { event.recycle() }
    }
    send(MotionEvent.ACTION_DOWN, start.toFloat())
    for (step in 1..10) {
        SystemClock.sleep((duration / 10).toLong())
        send(MotionEvent.ACTION_MOVE, start + (end - start) * step / 10f)
    }
    send(MotionEvent.ACTION_UP, end.toFloat())
    inst.waitForIdleSync()
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

// custom scroll
val customScrollTo: ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return CoreMatchers.allOf(withEffectiveVisibility(Visibility.VISIBLE), isDescendantOfA(anyOf(
            isAssignableFrom(ScrollView::class.java),
            isAssignableFrom(HorizontalScrollView::class.java),
            isAssignableFrom(NestedScrollView::class.java)))
        )
    }

    override fun getDescription(): String {
        return "Scroll to view"
    }

    override fun perform(uiController: UiController?, view: View?) {
        ScrollToAction().perform(uiController, view)
    }
}

fun Matcher<View>.customScrollToView(timeOutIfNotExitInSec: Int = 5) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(customScrollTo)
}

fun Matcher<View>.swipeRightOnView(timeOutIfNotExitInSec: Int = 5) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(ViewActions.swipeRight())
}

/**
 * Perform action of waiting for a certain view within a single root view
 * @param matcher Generic Matcher used to find our view
 */
fun searchFor(matcher: Matcher<View>): ViewAction {

    return object : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "searching for view $matcher in the root view"
        }

        override fun perform(uiController: UiController, view: View) {

            var tries = 0
            val childViews: Iterable<View> = TreeIterables.breadthFirstViewTraversal(view)

            // Look for the match in the tree of childviews
            childViews.forEach {
                tries++
                if (matcher.matches(it)) {
                    // found the view
                    return
                }
            }

            throw NoMatchingViewException.Builder()
                .withRootView(view)
                .withViewMatcher(matcher)
                .build()
        }
    }
}

/**
 * scroll to position in PagerView
 */
fun Matcher<View>.scrollToPositionInPagerView(position: Int, timeOutIfNotExitInSec: Int = 3) {
    waitForViewVisible(timeOutIfNotExitInSec)
    onView(this).perform(ViewPagerActions.scrollToPage(position))
}

/**
 *  items count in RecycleView
 */
fun Matcher<View>.getItemsCountFromRecyclerView(): Int {
    var count = 0
    val matcher: Matcher<View> = object : TypeSafeMatcher<View>() {
        override fun matchesSafely(item: View): Boolean {
            count = (item as RecyclerView).adapter!!.itemCount
            return true
        }

        override fun describeTo(description: Description) {
            description.appendText("should return items count")
        }
    }
    onView(first(this)).check(ViewAssertions.matches(matcher))
    val result = count
    count = 0
    return result
}

fun staticWait(timeOut: Int = 3) {
    try { Thread.sleep((timeOut * 1000).toLong()) } catch (_: InterruptedException) {}
}

//fun withViewCount(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View?>? {
//    return object : TypeSafeMatcher<View?>() {
//        var actualCount = -1
//        override fun describeTo(description: Description) {
//            if (actualCount >= 0) {
//                description.appendText("With expected number of items: $expectedCount")
//                description.appendText("\n With matcher: ")
//                viewMatcher.describeTo(description)
//                description.appendText("\n But got: $actualCount")
//            }
//        }
//
//        override fun matchesSafely(root: View?): Boolean {
//            actualCount = 0
//            val iterable = TreeIterables.breadthFirstViewTraversal(root)
//            actualCount = Iterables.filter(iterable, withMatcherPredicate(viewMatcher)).count()
//            return actualCount == expectedCount
//        }
//    }
//}

private fun withMatcherPredicate(matcher: Matcher<View>): Predicate<View?> {
    return Predicate<View?> { view -> matcher.matches(view) }
}

