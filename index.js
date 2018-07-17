import { NativeModules } from "react-native";

const { DumbleCamera } = NativeModules;

export default {
  startScan: function() {
    DumbleCamera.openCamera();
  },
  SCANNED_RESULT: DumbleCamera.SCANNED_RESULT
};
