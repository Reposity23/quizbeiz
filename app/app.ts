import { Application } from '@nativescript/core';
import { LaunchScreen } from './screens/LaunchScreen';

Application.run({ create: () => new LaunchScreen() });