import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const config = [
  ...nextVitals,
  ...nextTs,
  { ignores: [".next/**", "node_modules/**", "next-env.d.ts"] },
  // Template images and user previews may be blob URLs supplied at runtime.
  { rules: { "@next/next/no-img-element": "off" } },
];
export default config;
