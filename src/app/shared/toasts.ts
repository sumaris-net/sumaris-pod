import {ToastController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";
import {ToastOptions} from "@ionic/core";
import {ToastButton} from "@ionic/core/dist/types/components/toast/toast-interface";
import {isNotNil} from "./functions";

export class Toasts {
  static show = showToast;
}

export async function showToast(
  toastController: ToastController,
  translate: TranslateService,
  opts: ToastOptions & { error?: boolean; }
) {
  if (!toastController || !translate) throw new Error("Missing required argument 'toastController' or 'translate'");

  const i18nKeys = [opts.message];
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
    }
    else {
      closeButton = {role: 'close'};
      opts.buttons.push(closeButton);
    }
    closeButton.text = closeButton.text || 'COMMON.BTN_CLOSE';
    i18nKeys.push(closeButton.text);
  }

  if (opts.error) {
    const cssArray = opts.cssClass && typeof opts.cssClass === 'string' && opts.cssClass.split(',') || (opts.cssClass as Array<string>) || [];
    cssArray.push('error');
    opts.cssClass = cssArray;
  }

  const translations = await translate.instant(i18nKeys);

  if (closeButton) {
    closeButton.text = translations[closeButton.text];
  }

  const toast = await toastController.create({
    // Default values
    position: !this.mobile && 'top' || undefined,
    duration: 3000,
    ...opts,
    message: translations[opts.message],
    header: opts.header && translations[opts.header] || undefined
  });
  return toast.present();
}
