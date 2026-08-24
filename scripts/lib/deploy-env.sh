#!/usr/bin/env bash
#
# deploy-env.sh — maps PRD_*-prefixed overrides onto their canonical names.
#
# Sourced (never executed) by replit-build.sh and replit-run.sh. The .replit
# [deployment] section is the only caller of those two scripts; the Replit
# workspace boots the app through [workflows] instead and never sources this
# file. So the base variables hold the DEV Clerk instance and are what the
# workspace uses, while the published app gets the PRD_ values.
#
# To give the published app its own value for <NAME>, set PRD_<NAME> in
# Replit Configurations (or Secrets, when the value is sensitive). Leave
# PRD_<NAME> unset to share the base value across both environments.
#
# MUST be sourced BEFORE scripts/replit-env.sh: that script derives
# VITE_CLERK_PUBLISHABLE_KEY from CLERK_PUBLISHABLE_KEY, and Vite bakes the
# publishable key into the bundle at build time — a later override is too late.
#
# Sourced with `set -u` active, hence the `:-` guards on indirect lookups.

DEPLOY_OVERRIDABLE_VARS=(
	CLERK_PUBLISHABLE_KEY
	AUTH_AUTHORIZED_PARTIES
	APP_SECURITY_CORS_ALLOWED_ORIGINS
	APP_SECURITY_CSP_FRAME_ANCESTORS
)

for _deploy_var in "${DEPLOY_OVERRIDABLE_VARS[@]}"; do
	_deploy_src="PRD_${_deploy_var}"
	if [ -n "${!_deploy_src:-}" ]; then
		export "${_deploy_var}=${!_deploy_src}"
		# Log names only — a published value may carry a secret.
		echo "[deploy-env] ${_deploy_var} <- ${_deploy_src}"
	else
		echo "[deploy-env] WARNING: ${_deploy_src} is unset;" \
			"the published app will use the base ${_deploy_var}" >&2
	fi
done

unset _deploy_var _deploy_src
