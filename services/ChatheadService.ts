import { Application } from '@nativescript/core';

declare const android: any;

export class ChatheadService {
    private windowManager: any;
    private chatHead: any;
    private params: any;
    private isInitialized: boolean = false;

    async initialize() {
        if (!Application.android) return;
        if (this.isInitialized) return;

        const context = Application.android.context;
        this.windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE);

        this.chatHead = new android.widget.ImageView(context);
        const imageUrl = "profile.png";
        
        // Load image from local resources
        try {
            const resourceId = context.getResources().getIdentifier(
                'profile',
                'drawable',
                context.getPackageName()
            );
            this.chatHead.setImageResource(resourceId);
        } catch (e) {
            console.error('Error loading chat head image:', e);
            // Set a fallback color as background
            this.chatHead.setBackgroundColor(android.graphics.Color.parseColor('#4CAF50'));
        }

        // Set up window parameters with proper size
        const size = Math.round(48 * context.getResources().getDisplayMetrics().density);
        this.params = new android.view.WindowManager.LayoutParams(
            size,
            size,
            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        );

        this.params.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
        
        // Add touch listener for dragging and double tap
        this.setupTouchListener();
        
        // Add chat head to window
        try {
            this.windowManager.addView(this.chatHead, this.params);
            this.isInitialized = true;
        } catch (e) {
            console.error('Error adding chat head to window:', e);
        }
    }

    private setupTouchListener() {
        let initialX: number;
        let initialY: number;
        let initialTouchX: number;
        let initialTouchY: number;
        let lastTapTime = 0;

        this.chatHead.setOnTouchListener(new android.view.View.OnTouchListener({
            onTouch: (view: any, event: any) => {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = this.params.x;
                        initialY = this.params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        
                        // Handle double tap
                        const currentTime = new Date().getTime();
                        if (currentTime - lastTapTime < 300) {
                            this.handleDoubleTap();
                        }
                        lastTapTime = currentTime;
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        this.params.x = initialX + (event.getRawX() - initialTouchX);
                        this.params.y = initialY + (event.getRawY() - initialTouchY);
                        try {
                            this.windowManager.updateViewLayout(this.chatHead, this.params);
                        } catch (e) {
                            console.error('Error updating chat head position:', e);
                        }
                        return true;
                }
                return false;
            }
        }));
    }

    private handleDoubleTap() {
        // Trigger screen capture
        const context = Application.android.context;
        const intent = new android.content.Intent(context, context.getClass());
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    showMessage(message: string) {
        if (!Application.android) return;

        const context = Application.android.context;
        try {
            const toast = android.widget.Toast.makeText(
                context,
                message,
                android.widget.Toast.LENGTH_SHORT
            );
            toast.show();
        } catch (e) {
            console.error('Error showing toast:', e);
        }
    }
            try {
                const url = new java.net.URL(imageUrl);
                const connection = url.openConnection();
                const input = connection.getInputStream();
                const bitmap = android.graphics.BitmapFactory.decodeStream(input);
                this.chatHead.setImageBitmap(bitmap);
            } catch (e) {
                console.error('Error loading chat head image:', e);
            }
        });

        // Set up window parameters
        this.params = new android.view.WindowManager.LayoutParams(
            100,
            100,
            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        );

        this.params.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
        
        // Add touch listener for dragging
        this.setupDragListener();
        
        // Add chat head to window
        this.windowManager.addView(this.chatHead, this.params);
    }

    private setupDragListener() {
        let initialX: number;
        let initialY: number;
        let initialTouchX: number;
        let initialTouchY: number;

        this.chatHead.setOnTouchListener(new android.view.View.OnTouchListener({
            onTouch: (view: any, event: any) => {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = this.params.x;
                        initialY = this.params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        this.params.x = initialX + (event.getRawX() - initialTouchX);
                        this.params.y = initialY + (event.getRawY() - initialTouchY);
                        this.windowManager.updateViewLayout(this.chatHead, this.params);
                        return true;
                }
                return false;
            }
        }));
    }

    showMessage(message: string) {
        if (!Application.android) return;

        const context = Application.android.context;
        const toast = android.widget.Toast.makeText(
            context,
            message,
            android.widget.Toast.LENGTH_SHORT
        );
        toast.show();
    }
}