import {createAnimation, IonicSafeString, ToastController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";
import {OverlayEventDetail, ToastOptions} from "@ionic/core";
import {ToastButton} from "@ionic/core/dist/types/components/toast/toast-interface";
import {isNotNil} from "./functions";
import {AnimationBuilder} from "@angular/animations";

const TOAST_MAX_HEIGHT_PX = 75;
const TOAST_MAX_STACK_SIZE = 4;

export class Toasts {
  static counter = 0;
  static stackSize = 0;

  static async show<T = any>(
    toastController: ToastController,
    translate: TranslateService,
    opts: ToastOptions & { error?: boolean; showCloseButton?: boolean; }
  ): Promise<OverlayEventDetail<T>> {
    if (!toastController || !translate) throw new Error("Missing required argument 'toastController' or 'translate'");

    this.counter++; // Increment toast counter
    let currentOffset = this.stackSize;
    if (this.stackSize < TOAST_MAX_STACK_SIZE) {
      this.stackSize++; // Increment stack offset
    }
    else {
      this.stackSize = 1; // Reset the stack
      currentOffset = 0;
    }


    const message = opts.message && opts.message instanceof IonicSafeString ? opts.message.value : opts.message as string;
    const i18nKeys = [message];
    if (opts.header) i18nKeys.push(opts.header);

    let closeButton: ToastButton;
    if (opts.showCloseButton) {
      opts.buttons = opts.buttons || [];
      const buttonIndex = opts.buttons
        .map(b => typeof b === 'object' && b as ToastButton || undefined)
        .filter(isNotNil)
        .findIndex(b => b.role === 'close');
      if (buttonIndex !== -1) {
        closeButton = opts.buttons[buttonIndex] as ToastButton;
      } else {
        closeButton = {role: 'close'};
        opts.buttons.push(closeButton);
      }
      closeButton.text = closeButton.text || 'COMMON.BTN_OK';
      i18nKeys.push(closeButton.text);
    }

    // Read CSS
    const cssArray = opts.cssClass && typeof opts.cssClass === 'string' && opts.cssClass.split(',') || (opts.cssClass as Array<string>) || [];

    // Add 'error' class, if need
    if (opts.error && !cssArray.includes('danger')) {
      cssArray.push('danger');
    }

    opts.cssClass = cssArray;

    const translations = await translate.instant(i18nKeys);

    if (closeButton) {
      closeButton.text = translations[closeButton.text];
    }

    const position = opts.mode === 'ios' ? 'bottom' : 'top'; // Top by default

    if (!opts.enterAnimation) {
      // Compute Y end position, using the stack offset
      const direction = position === 'top' ? 1 : -1;
      const start = (currentOffset) * TOAST_MAX_HEIGHT_PX  - direction * TOAST_MAX_HEIGHT_PX;
      const end = direction * (currentOffset) * TOAST_MAX_HEIGHT_PX;
      opts.enterAnimation = (baseEl: any, opts?: any) => {
        return createAnimation()
          .addElement(baseEl.querySelector('.toast-wrapper'))
          .duration(250)
          .fromTo('transform', `translateY(${start}px)`, `translateY(${end}px)`)
          .fromTo('opacity', '0', '1');
      };
    }

    const toast = await toastController.create({
      // Default values
      position,
      duration: opts.showCloseButton ? 10000 : 4000,
      ...opts,
      message: translations[message],
      header: opts.header && translations[opts.header] || undefined
    });

    await toast.present();
    const result = await toast.onDidDismiss();

    // Decrease counter
    this.counter--;
    // If all toast closed: reset the stack offset
    if (this.counter === 0) {
      this.stackSize = 0;
    }
    // If current toast is the last one, decrease the offset
    else if (this.stackSize == currentOffset + 1){
      this.stackSize--;
    }

    return result;
  }
}
