import { Frame, Page, Label } from '@nativescript/core';
import { MainScreen } from './MainScreen';

export class LaunchScreen extends Page {
    constructor() {
        super();
        this.backgroundColor = '#4CAF50';
        
        const label = new Label();
        label.text = 'Success';
        label.className = 'success-text';
        label.horizontalAlignment = 'center';
        label.verticalAlignment = 'middle';
        
        this.content = label;
        
        // Use proper timeout handling
        const timeoutId = setTimeout(() => {
            Frame.topmost().navigate({
                create: () => new MainScreen()
            });
        }, 5000);
        
        // Clean up timeout if page is unloaded
        this.on('unloaded', () => {
            clearTimeout(timeoutId);
        });
    }
}