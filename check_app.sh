#!/bin/bash

echo "=== Vérification AudioOverlay Mix ==="
echo ""

echo "1. Application installée ?"
adb shell pm list packages | grep com.company.product
echo ""

echo "2. Service en cours d'exécution ?"
adb shell dumpsys activity services | grep -A 5 OverlayService
echo ""

echo "3. Permissions accordées ?"
adb shell dumpsys package com.company.product | grep -A 20 "granted=true"
echo ""

echo "4. Derniers logs d'erreur :"
adb logcat -d | grep -i "company.product" | tail -20
echo ""

echo "=== Fin du diagnostic ==="
