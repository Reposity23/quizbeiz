import { Page, Image, AndroidApplication, Application } from '@nativescript/core';
import { ChatheadService } from '../services/ChatheadService';
import { ScreenCaptureService } from '../services/ScreenCaptureService';
import { OpenAIService } from '../services/OpenAIService';

declare const android: any;

export class MainScreen extends Page {
    private chatheadService: ChatheadService;
    private screenCaptureService: ScreenCaptureService;
    private openAIService: OpenAIService;

    constructor() {
        super();
        
        this.chatheadService = new ChatheadService();
        this.screenCaptureService = new ScreenCaptureService();
        this.openAIService = new OpenAIService();
        
        this.initializeServices();
    }

    private async initializeServices() {
        // Request necessary permissions
        await this.requestPermissions();
        
        await this.chatheadService.initialize();
        await this.screenCaptureService.initialize();
        
        // Set up double tap handler for screen capture
        this.screenCaptureService.onRegionSelected((imageData) => {
            this.processImage(imageData);
        });
    }

    private async requestPermissions() {
        if (Application.android) {
            const context = Application.android.context;
            
            // Request SYSTEM_ALERT_WINDOW permission
            if (!android.provider.Settings.canDrawOverlays(context)) {
                const intent = new android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                );
                context.startActivity(intent);
            }
        }
    }

    private async processImage(imageData: string) {
        const text = await this.screenCaptureService.performOCR(imageData);
        // For testing, always show "no API" response
        this.chatheadService.showMessage('no API');
    }
}