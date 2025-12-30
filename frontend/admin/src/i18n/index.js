import i18n from "i18next";
import { initReactI18next } from "react-i18next";

// VI
import viCommon from "./locales/vi/common.json";
import viUser from "./locales/vi/user.json";

// EN
import enCommon from "./locales/en/common.json";
import enUser from "./locales/en/user.json";

const resources = {
    vi: {
        common: viCommon,
        user: viUser,
    },
    en: {
        common: enCommon,
        user: enUser,
    },
};

i18n.use(initReactI18next).init({
    resources,
    lng: localStorage.getItem("lang") || "vi",
    fallbackLng: "vi",
    ns: ["common", "user"],
    defaultNS: "common",
    interpolation: {
        escapeValue: false,
    },
});

export default i18n;
