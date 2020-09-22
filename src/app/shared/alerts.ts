import {AlertController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";

export class Alerts {

  static askSaveBeforeLeave = askSaveBeforeLeave;
  static askDeleteConfirmation = askDeleteConfirmation;
  static askActionConfirmation = askActionConfirmation;
  static askConfirmation = askConfirmation;
  static showError = showError;
}

/**
 * Ask the user to save before leaving. If return undefined: user has cancelled
 * @param alertCtrl
 * @param translate
 * @param event
 */
export async function askSaveBeforeLeave(
  alertCtrl: AlertController,
  translate: TranslateService,
  event?: UIEvent): Promise<boolean|undefined> {
  let confirm = false;
  let cancel = false;
  const translations = translate.instant(['COMMON.BTN_SAVE', 'COMMON.BTN_CANCEL', 'COMMON.BTN_ABORT_CHANGES', 'CONFIRM.SAVE', 'CONFIRM.ALERT_HEADER']);
  const alert = await alertCtrl.create({
    header: translations['CONFIRM.ALERT_HEADER'],
    message: translations['CONFIRM.SAVE'],
    buttons: [
      {
        text: translations['COMMON.BTN_CANCEL'],
        role: 'cancel',
        cssClass: 'secondary',
        handler: () => {
          cancel = true;
        }
      },
      {
        text: translations['COMMON.BTN_ABORT_CHANGES'],
        cssClass: 'secondary',
        handler: () => {
        }
      },
      {
        text: translations['COMMON.BTN_SAVE'],
        handler: () => {
          confirm = true; // update upper value
        }
      }
    ]
  });
  await alert.present();
  await alert.onDidDismiss();

  if (confirm) return true; // = Do save and leave

  if (cancel) {
    // Stop the event
    if (event) event.preventDefault();

    return undefined; // User cancelled
  }

  return false; // Leave without saving
}

/**
 * Ask the user to save before leaving. If return undefined: user has cancelled
 * @param alertCtrl
 * @param translate
 * @param event
 */
export async function askDeleteConfirmation(
  alertCtrl: AlertController,
  translate: TranslateService,
  event?: UIEvent): Promise<boolean|undefined> {
  let confirm = false;
  let cancel = false;
  const translations = translate.instant(['COMMON.BTN_YES_DELETE', 'COMMON.BTN_CANCEL', 'CONFIRM.DELETE', 'CONFIRM.ALERT_HEADER']);
  const alert = await alertCtrl.create({
    header: translations['CONFIRM.ALERT_HEADER'],
    message: translations['CONFIRM.DELETE'],
    buttons: [
      {
        text: translations['COMMON.BTN_CANCEL'],
        role: 'cancel',
        cssClass: 'secondary',
        handler: () => {
          cancel = true;
        }
      },
      {
        text: translations['COMMON.BTN_YES_DELETE'],
        handler: () => {
          confirm = true; // update upper value
        }
      }
    ]
  });
  await alert.present();
  await alert.onDidDismiss();

  if (confirm) return true; // = Do save and leave

  if (cancel) {
    // Stop the event
    if (event) event.preventDefault();

    return undefined; // User cancelled
  }

  return false; // Leave without saving
}

/**
 * Ask the user to conform an action. If return undefined: user has cancelled
 * @param alertCtrl
 * @param translate
 * @param immediate is action has an immediate effect ?
 * @param event
 */
export async function askActionConfirmation(
  alertCtrl: AlertController,
  translate: TranslateService,
  immediate?: boolean,
  event?: UIEvent): Promise<boolean|undefined> {
  const messageKey = immediate === true ? 'CONFIRM.ACTION_IMMEDIATE' : 'CONFIRM.ACTION';
  return askConfirmation(messageKey, alertCtrl, translate, event);
}

/**
 * Ask the user to confirm. If return undefined: user has cancelled
 * @pram messageKey i18n message key
 * @param alertCtrl
 * @param translate
 * @param event
 */
export async function askConfirmation(
  messageKey: string,
  alertCtrl: AlertController,
  translate: TranslateService,
  event?: UIEvent): Promise<boolean|undefined> {
  if (!alertCtrl || !translate) throw new Error("Missing required argument 'alertCtrl' or 'translate'");
  let confirm = false;
  let cancel = false;
  const translations = translate.instant(['COMMON.BTN_YES_CONTINUE', 'COMMON.BTN_CANCEL', messageKey, 'CONFIRM.ALERT_HEADER']);
  const alert = await alertCtrl.create({
    header: translations['CONFIRM.ALERT_HEADER'],
    message: translations[messageKey],
    buttons: [
      {
        text: translations['COMMON.BTN_CANCEL'],
        role: 'cancel',
        cssClass: 'secondary',
        handler: () => {
          cancel = true;
        }
      },
      {
        text: translations['COMMON.BTN_YES_CONTINUE'],
        handler: () => {
          confirm = true; // update upper value
        }
      }
    ]
  });
  await alert.present();
  await alert.onDidDismiss();

  if (confirm) return true; // = Do save and leave

  if (cancel) {
    // Stop the event
    if (event) event.preventDefault();

    return undefined; // User cancelled
  }

  return false; // Leave without saving
}


export async function showError(
  messageKey: string,
  alertCtrl: AlertController,
  translate: TranslateService, opts? : {
    titleKey?: string;
  }) {
  if (!messageKey || !alertCtrl || !translate) throw new Error("Missing a required argument ('messageKey', 'alertCtrl' or 'translate')");
  const titleKey = opts && opts.titleKey || 'ERROR.ALERT_HEADER';
  const translations = translate.instant(['COMMON.BTN_OK', messageKey, titleKey]);
  const alert = await alertCtrl.create({
    header: translations[titleKey],
    message: translations[messageKey],
    buttons: [
      {
        text: translations['COMMON.BTN_OK'],
        role: 'cancel'
      }
    ]
  });
  await alert.present();
  await alert.onDidDismiss();

}
