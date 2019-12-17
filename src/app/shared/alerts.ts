import {AlertController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";

export class Alerts {

  static askSaveBeforeLeave = askSaveBeforeLeave;
  static askDeleteConfirmation = askDeleteConfirmation;
  static askActionConfirmation = askActionConfirmation;
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
 * @param event
 */
export async function askActionConfirmation(
  alertCtrl: AlertController,
  translate: TranslateService,
  immediate: boolean,
  event?: UIEvent): Promise<boolean|undefined> {
  if (!alertCtrl || !translate) throw new Error("Missing required argument 'alertCtrl' or 'translate'");
  let confirm = false;
  let cancel = false;
  const messageKey = immediate ? 'CONFIRM.ACTION_IMMEDIATE' : 'CONFIRM.ACTION';
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

