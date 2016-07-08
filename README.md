# CarouselView

This library is updated version of [Android 3D Carousel](http://www.codeproject.com/Articles/146145/Android-3D-Carousel).

![Alt text](https://s31.postimg.org/h5zmkihzv/ezgif_com_video_to_gif.gif)

List of changes:
- Fixed touch gesture
- Improved performance
- Closed issue with battery life
- Added opportunity to add layout content
- New parameters for carousel view.


Optional for widget view:

    /**
     * Default min quantity of views.
     */
    static final int MIN_QUANTITY = 3;

    /**
     * Default max quantity of views.
     */
    static final int MAX_QUANTITY = 12;

    /**
     * Set diameter distortion, 1.0 = perfect circle
     */
    static final float DIAMETER_SCALE = 0.4f;
    
    /**
     * Rate to shrink objects as they appear further back in the depth field. Typical values 1.0,
     * linear, 2.0 twice as fast.
     */
    static  final float DEPTH_SCALE = 0.8f;
    
    /**
     * Tilt angle, negative lifts up back, positive lowers back.
     */
    static float TILT = -0.3f;  
    
    /**
     * Limit depth scale used to shrink far objects to not fall below this minimum scale.
     */
    static final float MIN_SCALE = 0.4f;        
    
    /**
     * Max velocity for scrolling.
     */
    static final int MAX_SCROLLING_VELOCITY = 16000;

    /**
     * Max scrolling distance.
     */
    static final int MAX_SCROLLING_DISTANCE = 13;

    /**
     * Duration in milliseconds from the start of a scroll during which we're
     * unsure whether the user is scrolling or flinging.
     */
    static final int SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT = 100;

    /**
     * Duration in milliseconds from the start of animation to end.
     */
    static final int ANIMATION_DURATION = 200;

    /**
     * Default value for rotation scroll threshold.
     */
    static final int SCROLLING_THRESHOLD = 150;

    /**
     * Default min alpha value.
     */
    static final int MIN_ALPHA = 30;  

    /**
     * Defines default selected item.
     */
    static final int DEFAULT_SELECTED_ITEM = 0;
    
    /**
     * Configures size of items which are not in front.
     */
    static final int CAROUSEL_ITEM_Z_POSITION = 1;
    
    /**
     * Configures vertical shift of non-front items.
     */
    static final float CAROUSEL_ITEM_Y_POSITION= 1.0f;


