package me.proton.android.lumo.webview

import android.util.Log
import android.webkit.WebView
import me.proton.android.lumo.config.LumoConfig

private const val TAG = "JsInjector"

/**
 * Injects essential JavaScript into the WebView to monitor Lumo container visibility
 * and handle navigation.
 */
fun injectEssentialJavascript(webView: WebView) {
    val js = """
        (function() {
            // Function to check if current page is a Lumo page
            function isLumoPage() {
                const hasLumoContainer = document.querySelector('.lumo-input-container') !== null;
                const currentUrl = window.location.href;
                const isLumoDomain = currentUrl.includes('${LumoConfig.LUMO_DOMAIN}');    
                return hasLumoContainer || isLumoDomain;
            }
            
            // Function to notify Android about page type
            function notifyPageType() {
                const isLumo = isLumoPage();
                if (window.Android && typeof window.Android.onPageTypeChanged === 'function') {
                    const currentUrl = window.location.href;
                    window.Android.onPageTypeChanged(isLumo, currentUrl);
                }
            }
            
            // Only run container check logic on lumo domain
            const currentUrl = window.location.href;
            const isLumoDomain = currentUrl.includes('${LumoConfig.LUMO_DOMAIN}');
            if (isLumoDomain) {
                // Function to check for Lumo container
                function checkLumoContainer() {
                    console.log('Checking for Lumo container...');
                    const container = document.querySelector('.lumo-input-container');
                    console.log('Container found:', container);
                    
                    if (container) {
                        console.log('Lumo container found, notifying Android');
                        // Unhide the voice entry button if present
                        var voiceBtn = document.getElementById('voice-entry-mobile');
                        if (voiceBtn && voiceBtn.classList.contains('hidden')) {
                            voiceBtn.classList.remove('hidden');
                            console.log('Unhid #voice-entry-mobile');
                        }
                        
                        // Hide the prompt entry text if present
                        const promptTextElements = document.getElementsByClassName('prompt-entry-hint');
                        for (let element of promptTextElements) {
                            element.style.setProperty('display', 'none', 'important');
                        }
                        
                        // Add click handler to #voice-entry-mobile-button
                        var voiceBtnAction = document.getElementById('voice-entry-mobile-button');
                        if (voiceBtnAction && !voiceBtnAction.hasAttribute('data-android-handler')) {
                            voiceBtnAction.addEventListener('click', function(e) {
                                console.log('Voice entry button clicked, calling Android.startVoiceEntry()');
                                if (window.Android && typeof window.Android.startVoiceEntry === 'function') {
                                    window.Android.startVoiceEntry();
                                }
                            });
                            voiceBtnAction.setAttribute('data-android-handler', 'true');
                        }
                        if (window.Android && typeof window.Android.onLumoContainerVisible === 'function') {
                            console.log('Calling onLumoContainerVisible');
                            window.Android.onLumoContainerVisible();
                        } else {
                            console.error('Android interface or onLumoContainerVisible method not found');
                        }
                        return true;
                    }
                    console.log('Lumo container not found');
                    return false;
                }
                
                // Run immediately on page load
                console.log('Running initial page checks...');
                notifyPageType();
                checkLumoContainer();
                
                // Set up observer to detect when Lumo container appears/disappears
                if (!window._lumoContainerObserver) {
                    console.log('Setting up Lumo container observer...');
                    let debounceTimer = null;
                    
                    const observer = new MutationObserver((mutations) => {
                        // Debounce to prevent excessive checks
                        if (debounceTimer) clearTimeout(debounceTimer);
                        debounceTimer = setTimeout(() => {
                            console.log('DOM mutation detected, checking container...');
                            notifyPageType();
                            checkLumoContainer();
                        }, 100); // 100ms debounce
                    });
                    
                    observer.observe(document.body, { 
                        childList: true, 
                        subtree: true 
                    });
                    
                    window._lumoContainerObserver = observer;
                    console.log('Lumo container observer started');
                }
                
                // Override history methods to track navigation
                const originalPushState = history.pushState;
                const originalReplaceState = history.replaceState;
                
                function cleanupIntervals() {
                    if (window._promotionButtonInterval) {
                        clearInterval(window._promotionButtonInterval);
                        window._promotionButtonInterval = null;
                        console.log('Cleared promotion button interval on navigation');
                    }
                    if (window._upgradeLinkInterval) {
                        clearInterval(window._upgradeLinkInterval);
                        window._upgradeLinkInterval = null;
                        console.log('Cleared upgrade link interval on navigation');
                    }
                }
                
                history.pushState = function() {
                    console.log('Navigation push detected');
                    cleanupIntervals();
                    originalPushState.apply(this, arguments);
                    if (window.Android && typeof window.Android.onNavigation === 'function') {
                        window.Android.onNavigation(window.location.href, 'push');
                    }
                    // Check for container after a short delay
                    setTimeout(checkLumoContainer, 100);
                };
                
                history.replaceState = function() {
                    console.log('Navigation replace detected');
                    cleanupIntervals();
                    originalReplaceState.apply(this, arguments);
                    if (window.Android && typeof window.Android.onNavigation === 'function') {
                        window.Android.onNavigation(window.location.href, 'replace');
                    }
                    // Check for container after a short delay
                    setTimeout(checkLumoContainer, 100);
                };
                
                // Listen for popstate events
                window.addEventListener('popstate', function() {
                    console.log('Navigation pop detected');
                    cleanupIntervals();
                    if (window.Android && typeof window.Android.onNavigation === 'function') {
                        window.Android.onNavigation(window.location.href, 'pop');
                    }
                    // Check for container after a short delay
                    setTimeout(checkLumoContainer, 100);
                });

                // Also check on URL changes
                let lastUrl = window.location.href;
                new MutationObserver(() => {
                    const currentUrl = window.location.href;
                    if (currentUrl !== lastUrl) {
                        console.log('URL changed from ' + lastUrl + ' to ' + currentUrl);
                        lastUrl = currentUrl;
                        notifyPageType();
                        // Check for container after a short delay
                        setTimeout(checkLumoContainer, 100);
                    }
                }).observe(document, { subtree: true, childList: true });

                // Periodic check for container (every 500ms for 5 seconds)
                let checkCount = 0;
                const maxChecks = 10;
                const checkInterval = setInterval(() => {
                    if (checkCount >= maxChecks) {
                        console.log('Max periodic checks reached, stopping interval');
                        clearInterval(checkInterval);
                        return;
                    }
                    console.log('Running periodic container check:', checkCount + 1);
                    if (checkLumoContainer()) {
                        console.log('Container found in periodic check, stopping interval');
                        clearInterval(checkInterval);
                    }
                    checkCount++;
                }, 500);

                // Define insertPromptAndSubmit globally
                window.insertPromptAndSubmit = function(prompt) {
                    console.log('Attempting to insert prompt: ' + prompt);
                    
                    // Start programmatic operation to prevent keyboard positioning
                    if (window.startProgrammaticOperation) {
                        window.startProgrammaticOperation();
                    }

                    function tryInsertPrompt(attempts = 0, maxAttempts = 10) {
                        const editor = document.querySelector('.tiptap.ProseMirror.composer');
                        if (editor) {
                            console.log('Editor found, inserting prompt');
                            editor.innerHTML = '';
                            const p = document.createElement('p');
                            p.textContent = prompt.trim(); 
                            editor.appendChild(p);
                            editor.focus();
                            
                            if (window.endProgrammaticOperation) {
                                window.endProgrammaticOperation();
                            }
                            
                            return true;
                        } else if (attempts < maxAttempts) {
                            console.log('Editor not found, retrying... (attempt ' + (attempts + 1) + ' of ' + maxAttempts + ')');
                            setTimeout(() => tryInsertPrompt(attempts + 1, maxAttempts), 500);
                            return false;
                        } else {
                            console.log('Failed to find editor after ' + maxAttempts + ' attempts');
                            
                            // End programmatic operation even on failure
                            if (window.endProgrammaticOperation) {
                                window.endProgrammaticOperation();
                            }
                            return false;
                        }
                    }
                    return tryInsertPrompt();
                };
                console.log('insertPromptAndSubmit function defined.');
            } else {
                // Not lumo domain, just notify page type
                notifyPageType();
            }
        })();
    """.trimIndent()

    Log.d(TAG, "Injecting essential JavaScript")
    webView.evaluateJavascript(js, null)
}

/**
 * Injects JavaScript to handle promotion buttons for payment
 */
fun injectPromotionButtonHandlers(webView: WebView) {
    val js = """
        (function() {
            try {
                console.log('Setting up promotion button click handlers');
                
                // Function to attach click handlers to promotion buttons
                function attachPromotionClickHandlers() {
                    // Select all buttons with the specified classes
                    const promotionButtons = document.querySelectorAll('.lumo-upgrade-trigger, .lumo-addon-button, .upsell-addon-button, .manage-plan');
                    
                    if (promotionButtons.length > 0) {
                        
                        // Add click handlers to each button
                        promotionButtons.forEach((button, index) => {
                            // Ensure we don't add multiple handlers to the same button
                            if (!button.hasAttribute('data-payment-handler')) {
                                button.addEventListener('click', function(e) {
                                    console.log(`Promotion button clicked`);
                                    e.preventDefault();
                                    e.stopPropagation();
                                    
                                    // Use robust AndroidInterface.showPayment() with retry logic
                                    if (window.AndroidInterface && window.AndroidInterface.showPayment) {
                                        window.AndroidInterface.showPayment();
                                    } else if (window.Android && window.Android.showPayment) {
                                        // Fallback to direct call if polyfill not loaded
                                        window.Android.showPayment();
                                    } else {
                                        console.error('Android payment interface not available');
                                        alert('Payment interface not available. Please refresh the page and try again.');
                                    }
                                    return false;
                                });
                                // Mark the button as having a handler
                                button.setAttribute('data-payment-handler', 'true');
                                console.log(`Added payment handler to promotion button`);
                            }
                        });
                    } else {
                        console.log('No promotion buttons found yet');
                    }
                }
                
                // Run immediately for any buttons that already exist
                attachPromotionClickHandlers();
                
                // Set up a MutationObserver to watch for dynamically added buttons
                if (!window._promotionButtonObserver) {
                    const observer = new MutationObserver((mutations) => {
                        let shouldCheckForButtons = false;
                        
                        for (const mutation of mutations) {
                            if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                                shouldCheckForButtons = true;
                                break;
                            }
                        }
                        
                        if (shouldCheckForButtons) {
                            console.log('DOM changed, checking for new promotion buttons');
                            attachPromotionClickHandlers();
                        }
                    });
                    
                    // Start observing the document body for DOM changes
                    observer.observe(document.body, { 
                        childList: true, 
                        subtree: true 
                    });
                    
                    window._promotionButtonObserver = observer;
                    console.log('Promotion button observer started');
                }
            } catch (e) {
                console.error('Error setting up promotion button handlers:', e);
                console.error('Error stack:', e.stack);
            }
        })();
    """.trimIndent()

    Log.d(TAG, "Injecting promotion button handlers JavaScript")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "Promotion button handler JS evaluation result: $result")
    }
}

fun injectSignupPlanParamFix(webView: WebView) {
    val js = """
        (function() {
            
            function updateSignupLinks() {
            
                var allLinks = document.querySelectorAll('a[href*="signup"]');
                
                var modifiedCount = 0;
                
                allLinks.forEach(function(link) {
                    var href = link.getAttribute('href');
                    var linkText = link.textContent || link.innerText || '[no text]';
                    
                    if (href && href.includes('signup') && !href.includes('plan=')) {
                        var newHref;
                        if (href.includes('?')) {
                            newHref = href + '&plan=free';
                        } else {
                            newHref = href + '?plan=free';
                        }
                        
                        link.setAttribute('href', newHref);
                        modifiedCount++;
                        
                        // This seems overly convoluted since it is
                        // I was having trouble getting the search param to stick
                        if (!link.hasAttribute('data-plan-click-handler')) {
                            link.addEventListener('click', function(e) {        
                                // ALWAYS prevent default and force our own navigation for signup links
                                e.preventDefault();
                                e.stopPropagation();
                                
                                var finalHref = this.getAttribute('href') || this.href;
                                
                                if (finalHref && finalHref.includes('signup') && !finalHref.includes('plan=')) {
                                    finalHref = finalHref.includes('?') ? finalHref + '&plan=free' : finalHref + '?plan=free';
                                }
                                
                                window.location.href = finalHref;
                                return false;
                            }, true); // Use capture phase to run before other handlers
                            
                            link.setAttribute('data-plan-click-handler', 'true');
                        }
                    }
                });
                
                return modifiedCount;
            }
            
            // Run immediately
            var initialCount = updateSignupLinks();
            
            var observer = new MutationObserver(function(mutations) {
                updateSignupLinks();
            });
            
            observer.observe(document.body, { childList: true, subtree: true });
        })();
    """.trimIndent()

    Log.d(TAG, "Injecting signup URL modifier JavaScript")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "Signup URL modifier JS evaluation result: $result")
    }
}

/**
 * Injects JavaScript to check for Lumo container visibility
 */
fun injectLumoContainerCheck(webView: WebView) {
    val js = """
        (function() {
            function checkLumoContainer() {
                const container = document.querySelector('.lumo-input-container');
                if (container) {
                    // Use a timeout to ensure Android interface is fully available
                    setTimeout(() => {
                        if (window.Android && typeof window.Android.onLumoContainerVisible === 'function') {
                            try {
                                window.Android.onLumoContainerVisible();
                            } catch (e) {
                                console.error('Error calling Android.onLumoContainerVisible:', e);
                            }
                        } else {
                            console.error('Android interface or onLumoContainerVisible method not found');
                        }
                    }, 150);
                    return true;
                }
                return false;
            }
            
            // Check immediately
            if (!checkLumoContainer()) {
                const observer = new MutationObserver((mutations) => {
                    if (checkLumoContainer()) {
                        observer.disconnect();
                    }
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
                
                // Safety timeout to prevent infinite loading
                setTimeout(() => {
                    observer.disconnect();
                    if (!checkLumoContainer()) {
                        if (window.Android && typeof window.Android.onLumoContainerVisible === 'function') {
                            try {
                                window.Android.onLumoContainerVisible();
                            } catch (e) {
                                console.error('Error calling Android.onLumoContainerVisible in safety timeout:', e);
                            }
                        }
                    }
                }, 3000);
            }
        })();
    """.trimIndent()

    Log.d(TAG, "Injecting Lumo container check JavaScript")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "Container check JS evaluation result: $result")
    }
}

/**
 * Injects JavaScript to handle links containing 'lumo/upgrade' in the href
 */
fun injectUpgradeLinkHandlers(webView: WebView) {

    val js = """
        (function() {
            try {
                // Function to attach click handlers to upgrade links
                function attachUpgradeLinkHandlers() {
                    // Select all links that contain 'lumo/upgrade' in the href
                    const upgradeLinks = document.querySelectorAll('a[href*="lumo/upgrade"]');
                    
                    if (upgradeLinks.length > 0) {
                        console.log(`Found upgrade links`);
                        
                        // Add click handlers to each link
                        upgradeLinks.forEach((link, index) => {
                            // Ensure we don't add multiple handlers to the same link
                            if (!link.hasAttribute('data-payment-handler')) {
                                link.addEventListener('click', function(e) {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    
                                    // Use robust AndroidInterface.showPayment() with retry logic
                                    if (window.AndroidInterface && window.AndroidInterface.showPayment) {
                                        window.AndroidInterface.showPayment();
                                    } else if (window.Android && window.Android.showPayment) {
                                        // Fallback to direct call if polyfill not loaded
                                        window.Android.showPayment();
                                    } else {
                                        console.error('Android payment interface not available');
                                        alert('Payment interface not available. Please refresh the page and try again.');
                                    }
                                    return false;
                                });
                                link.setAttribute('data-payment-handler', 'true');
                            }
                        });
                    } 
                }
                
                // Run immediately for any links that already exist
                attachUpgradeLinkHandlers();
                
                // Set up a MutationObserver to watch for dynamically added links
                if (!window._upgradeLinkObserver) {
                    const observer = new MutationObserver((mutations) => {
                        let shouldCheckForLinks = false;
                        
                        for (const mutation of mutations) {
                            if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                                shouldCheckForLinks = true;
                                break;
                            }
                        }
                        
                        if (shouldCheckForLinks) {
                            attachUpgradeLinkHandlers();
                        }
                    });
                    
                    // Start observing the document body for DOM changes
                    observer.observe(document.body, { 
                        childList: true, 
                        subtree: true 
                    });
                    
                    window._upgradeLinkObserver = observer;
                }
            } catch (e) {
                console.error('Error setting up upgrade link handlers:', e);
                console.error('Error stack:', e.stack);
            }
        })();
    """.trimIndent()
    Log.d(TAG, "Injecting upgrade link handlers JavaScript")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "Upgrade link handler JS evaluation result: $result")
    }
}

/**
 * Injects JavaScript to handle keyboard visibility and adjust composer positioning
 */
fun injectKeyboardHandling(webView: WebView) {
    Log.d(TAG, "🎯 injectKeyboardHandling() called - about to inject JavaScript")
    val js = """
        (function() {
            console.log('🚀 Lumo keyboard handler initializing...');
            
            // Define function immediately to prevent race conditions
            window.onNativeKeyboardChange = function(isVisible, dynamicKeyboardHeight) {
                console.log('🔥 JavaScript received keyboard event (early):', {isVisible, dynamicKeyboardHeight});
                // Store the call for when the full handler is ready
                window._pendingKeyboardCall = {isVisible, dynamicKeyboardHeight, timestamp: Date.now()};
            };
            
            let isKeyboardVisible = false;
            let composerElement = null;
            let keyboardOffset = 0; // Will be set by Android detection
            let isProgrammaticOperation = false; // Flag to prevent false triggers
            
            // Helper function to check if element is a valid composer input
            function isComposerInput(element) {
                if (!element) {
                    return false;
                }
                
                // Explicitly exclude buttons and navigation elements
                if (element.tagName === 'BUTTON' || 
                    element.classList.contains('button') || 
                    element.classList.contains('hamburger') ||
                    element.closest('button') ||
                    element.closest('.button')) {
                    return false;
                }
                
                // Only trigger for the actual composer input - be very specific
                const isComposerTextInput = (
                    (element.contentEditable === 'true' && element.classList.contains('ProseMirror') && element.classList.contains('composer')) ||
                    (element.contentEditable === 'true' && element.classList.contains('composer')) ||
                    (element.tagName === 'TEXTAREA' && element.closest('.lumo-input'))
                );
                
                // Must be within the lumo-input area specifically, not just the container
                const result = isComposerTextInput && element.closest('.lumo-input') !== null;
                return result;
            }
            
            // Function to find and cache the composer element
            function findComposerElement() {
                if (!composerElement) {
                    composerElement = document.querySelector('.lumo-input-container');
                }
                return composerElement;
            }
            
            // Show composer above keyboard
            function forceComposerAboveKeyboard() {
                const composer = findComposerElement();
                if (!composer) {
                    return false;
                }
                
                // Calculate safe positioning with bounds checking
                const windowHeight = window.innerHeight;
                const maxAllowedPosition = windowHeight * 0.7; // Don't go higher than 50% of window height
                const minBottomPosition = 250; // Minimum distance from bottom
                
                // Validate keyboard offset
                let safeKeyboardOffset = keyboardOffset;
                if (safeKeyboardOffset > maxAllowedPosition) {
                    console.warn('🚨 Keyboard offset too high:', safeKeyboardOffset, 'px. Capping to:', maxAllowedPosition, 'px');
                    safeKeyboardOffset = maxAllowedPosition;
                }
                
                // Ensure minimum positioning
                safeKeyboardOffset = Math.max(safeKeyboardOffset, minBottomPosition);
                
                const finalBottomPosition = safeKeyboardOffset + 0; // Add 8px padding above keyboard
                
                console.log('📱 Composer positioning:');
                console.log('  - Original keyboard offset: ' + keyboardOffset + 'px');
                console.log('  - Safe keyboard offset: ' + safeKeyboardOffset + 'px');
                console.log('  - Final bottom position: ' + finalBottomPosition + 'px');
                console.log('  - Window height: ' + windowHeight + 'px');
                console.log('  - Position ratio: ' + (finalBottomPosition / windowHeight * 100).toFixed(1) + '%');
                 
                composer.style.position = 'fixed';
                composer.style.bottom = finalBottomPosition + 'px';
                composer.style.left = '0';
                composer.style.right = '0';
                composer.style.width = '100%';
                composer.style.zIndex = '99999';
                composer.style.transform = 'translateY(0)';
                composer.style.transition = 'bottom 0.2s cubic-bezier(0.4, 0.0, 0.2, 1), transform 0.2s cubic-bezier(0.4, 0.0, 0.2, 1)';
                composer.style.boxShadow = '0 -2px 10px rgba(0,0,0,0.2)';
                composer.classList.add('keyboard-forced-visible');
                
                return true;
            }
            
            // Hide composer
            function resetComposerPosition() {
                const composer = findComposerElement();
                if (!composer) return;
                
                console.log('🔄 Resetting composer position');
                
                composer.style.transition = 'bottom 0.15s cubic-bezier(0.4, 0.0, 0.2, 1), transform 0.15s cubic-bezier(0.4, 0.0, 0.2, 1)';
                composer.style.position = '';
                composer.style.bottom = '';
                composer.style.left = '';
                composer.style.right = '';
                composer.style.width = '';
                composer.style.zIndex = '';
                composer.style.transform = '';
                composer.style.backgroundColor = '';
                composer.style.boxShadow = '';
                
                setTimeout(() => {
                    if (composer) {
                        composer.style.transition = '';
                        composer.classList.remove('keyboard-forced-visible');
                    }
                }, 150);
            }
            
            // Initialize everything
            function initKeyboardHandling() {
                
                // CSS for essential keyboard positioning styles
                const style = document.createElement('style');
                style.id = 'keyboard-forced-styles';
                style.textContent = `
                    .keyboard-forced-visible {
                        margin: 0 !important;
                        box-sizing: border-box !important;
                        max-width: 100vw !important;
                    }
                `;
                document.head.appendChild(style);
                
                // Replace temporary function with full handler
                window.onNativeKeyboardChange = function(isVisible, dynamicKeyboardHeight) {
                    // Skip if this is a programmatic operation (voice entry, etc.)
                    if (isProgrammaticOperation) {
                        console.log('📱 Skipping keyboard positioning during programmatic operation');
                        return;
                    }
                    
                    // Use precise Android keyboard height measurement
                    if (dynamicKeyboardHeight && dynamicKeyboardHeight > 0) {
                        keyboardOffset = dynamicKeyboardHeight;
                    } else {
                        console.log('⚠️ No valid keyboard height received:', dynamicKeyboardHeight);
                    }
                    
                    // Continue with existing keyboard logic...
                    handleKeyboardChange(isVisible, dynamicKeyboardHeight);
                };
                
                // Check for any pending keyboard calls that arrived early
                if (window._pendingKeyboardCall) {
                    console.log('📞 Processing pending keyboard call:', window._pendingKeyboardCall);
                    const pending = window._pendingKeyboardCall;
                    delete window._pendingKeyboardCall;
                    window.onNativeKeyboardChange(pending.isVisible, pending.dynamicKeyboardHeight);
                }
                
                // Extract keyboard logic into separate function
                function handleKeyboardChange(isVisible, dynamicKeyboardHeight) {
                    
                    // Handle welcome section visibility
                    function handleWelcomeSectionVisibility(keyboardVisible) {
                        const welcomeSection = document.querySelector('.lumo-welcome-section');
                        if (welcomeSection) {
                            if (keyboardVisible) {
                                console.log('📱 Hiding welcome section for keyboard');
                                welcomeSection.style.transition = 'opacity 0.2s ease-out';
                                welcomeSection.style.opacity = '0';
                                // Optional: hide completely after fade
                                setTimeout(() => {
                                    if (welcomeSection.style.opacity === '0') {
                                        welcomeSection.style.display = 'none';
                                    }
                                }, 200);
                            } else {
                                console.log('📱 Showing welcome section - keyboard hidden');
                                welcomeSection.style.display = '';
                                welcomeSection.style.transition = 'opacity 0.2s ease-in';
                                welcomeSection.style.opacity = '1';
                            }
                        }
                    }
                    
                    if (isVisible) {
                        console.log('📱 Keyboard is visible, checking if activeElement is composer input...');
                        // Hide welcome section immediately when keyboard appears
                        handleWelcomeSectionVisibility(true);
                        
                        if (isComposerInput(document.activeElement)) {
                            
                            // Add a delay to ensure focus is intentional and sustained
                            setTimeout(() => {
                                if (isComposerInput(document.activeElement)) {
                                    console.log('📱 Focus sustained - positioning composer');
                                    isKeyboardVisible = true;
                                    forceComposerAboveKeyboard();
                                } else {
                                    console.log('📱 Focus lost quickly - ignoring brief focus event');
                                }
                            }, 150); // 150ms delay to filter out brief touches
                        } else {
                            console.log('📱 ActiveElement is NOT composer input - ignoring keyboard event');
                        }
                    } else {
                        console.log('📱 Keyboard is hidden - resetting composer position');
                        isKeyboardVisible = false;
                        resetComposerPosition();
                        // Show welcome section when keyboard is hidden
                        handleWelcomeSectionVisibility(false);
                    }
                } // End of handleKeyboardChange function
                
                // Global functions to control programmatic operations
                window.startProgrammaticOperation = function() {
                    isProgrammaticOperation = true;
                };
                
                window.endProgrammaticOperation = function() {
                    isProgrammaticOperation = false;
                    // Force reset composer position in case it got stuck
                    if (isKeyboardVisible) {
                        isKeyboardVisible = false;
                        resetComposerPosition();
                    }
                };
                
                // Debug function to help diagnose keyboard positioning issues
                window.debugKeyboardPositioning = function() {
                    const composer = findComposerElement();
                    const debugInfo = {
                        timestamp: new Date().toISOString(),
                        windowHeight: window.innerHeight,
                        windowWidth: window.innerWidth,
                        keyboardOffset: keyboardOffset,
                        isKeyboardVisible: isKeyboardVisible,
                        isProgrammaticOperation: isProgrammaticOperation,
                        composerFound: !!composer,
                        activeElement: document.activeElement ? {
                            tagName: document.activeElement.tagName,
                            className: document.activeElement.className,
                            id: document.activeElement.id
                        } : null
                    };
                    
                    if (composer) {
                        const computedStyle = window.getComputedStyle(composer);
                        debugInfo.composerStyle = {
                            position: computedStyle.position,
                            bottom: computedStyle.bottom,
                            left: computedStyle.left,
                            right: computedStyle.right,
                            width: computedStyle.width,
                            zIndex: computedStyle.zIndex,
                            transform: computedStyle.transform,
                            visibility: computedStyle.visibility,
                            display: computedStyle.display
                        };
                        
                        const rect = composer.getBoundingClientRect();
                        debugInfo.composerRect = {
                            top: rect.top,
                            bottom: rect.bottom,
                            left: rect.left,
                            right: rect.right,
                            width: rect.width,
                            height: rect.height
                        };
                    }
                    
                    console.log('🔍 Keyboard Positioning Debug Info:', debugInfo);
                    return debugInfo;
                };
                
                // Focus handlers as backup method
                function setupComposerListeners() {
                    const composer = findComposerElement();
                    if (!composer) return false;
                    
                    // Only target the actual composer inputs, not all inputs in the container
                    const inputs = composer.querySelectorAll('.lumo-input .composer, .lumo-input textarea, .lumo-input .ProseMirror.composer');
                    
                    inputs.forEach(input => {
                        if (input.hasAttribute('data-keyboard-listener')) return;
                        
                        input.addEventListener('focusin', function() {
                            if (isProgrammaticOperation) {
                                console.log('📱 Skipping focus handler during programmatic operation');
                                return;
                            }
                            if (!isKeyboardVisible) {
                                setTimeout(() => {
                                    if (isComposerInput(document.activeElement) && !isKeyboardVisible) {
                                        isKeyboardVisible = true;
                                        forceComposerAboveKeyboard();
                                    }
                                }, 150);
                            }
                        });
                        
                        input.addEventListener('focusout', function() {
                            if (isProgrammaticOperation) {
                                return;
                            }
                            
                            setTimeout(() => {
                                const stillInComposer = isComposerInput(document.activeElement);
                                if (!stillInComposer && isKeyboardVisible) {
                                    isKeyboardVisible = false;
                                    resetComposerPosition();
                                }
                            }, 50);
                        });
                        
                        input.setAttribute('data-keyboard-listener', 'true');
                    });
                    
                    return inputs.length > 0;
                }
                
                // Set up listeners initially
                setupComposerListeners();
                
                // Observer for dynamic content
                const observer = new MutationObserver(function(mutations) {
                    let foundNewComposer = false;
                    let debounceTimer = null;
                    
                    // Clear existing timer
                    if (debounceTimer) clearTimeout(debounceTimer);
                    
                    // Debounce the check
                    debounceTimer = setTimeout(() => {
                        mutations.forEach(function(mutation) {
                            if (mutation.type === 'childList') {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.nodeType === 1 && node.querySelector && node.querySelector('.lumo-input-container')) {
                                        composerElement = null; // Reset cache
                                        foundNewComposer = true;
                                    }
                                });
                            }
                        });
                        
                        if (foundNewComposer) {
                            setupComposerListeners();
                            if (isKeyboardVisible) {
                                setTimeout(() => forceComposerAboveKeyboard(), 100);
                            }
                        }
                    }, 150); // 150ms debounce for composer changes
                });
                
                // Ensure document.body exists before observing
                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                } else {
                    console.log('⚠️ document.body not ready, deferring MutationObserver setup');
                    document.addEventListener('DOMContentLoaded', function() {
                        if (document.body) {
                            observer.observe(document.body, { childList: true, subtree: true });
                        }
                    });
                }
            }
            
            // Initialize when DOM is ready
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initKeyboardHandling);
            } else {
                initKeyboardHandling();
            }
            
            // Ensure function is immediately available for Android calls
            console.log('✅ Lumo keyboard handler ready. Function available:', typeof window.onNativeKeyboardChange);
     
        })();
    """.trimIndent()

    Log.d(TAG, "💉 About to inject keyboard JavaScript of length: ${js.length}")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "✅ Keyboard JavaScript injection completed. Result: $result")
    }
}

/**
 * Injects JavaScript to modify account pages by removing unwanted sections
 */
fun injectAccountPageModifier(webView: WebView) {
    val js = """
        (function() {
            function removeYourPlanSection() {
                var yourPlanSection = document.querySelector('#your-plan');
                if (yourPlanSection) {
                    yourPlanSection.remove();
                    console.log('Removed #your-plan section from account page');
                    return true;
                }
                return false;
            }
            
            // Run immediately
            removeYourPlanSection();
            
            // Set up observer for dynamically loaded content
            var observer = new MutationObserver(function(mutations) {
                removeYourPlanSection();
            });
            
            observer.observe(document.body, { childList: true, subtree: true });
        })();
    """.trimIndent()

    Log.d(TAG, "Injecting account page modifier JavaScript")
    webView.evaluateJavascript(js, null)
}

/**
 * Injects Android interface availability checking and retry logic to make interface calls robust
 */
fun injectAndroidInterfacePolyfill(webView: WebView) {
    val js = """
        (function() {
            // Create a robust wrapper for Android interface calls
            window.AndroidInterface = {
                // Maximum retry attempts
                maxRetries: 10,
                
                // Retry delay in milliseconds
                retryDelay: 200,
                
                // Check if Android interface is available
                isAvailable: function() {
                    return typeof window.Android !== 'undefined' && 
                           typeof window.Android.showPayment === 'function';
                },
                
                // Call showPayment with retry logic
                showPayment: function() {
                    if (this.isAvailable()) {
                        console.log('Android.showPayment available, calling immediately');
                        try {
                            window.Android.showPayment();
                            return true;
                        } catch (e) {
                            console.error('Error calling Android.showPayment:', e);
                            return false;
                        }
                    } else {
                        console.log('Android.showPayment not available, attempting retry logic');
                        this.retryShowPayment(0);
                        return false;
                    }
                },
                
                // Retry showPayment call with exponential backoff
                retryShowPayment: function(attempt) {
                    const self = this;
                    
                    if (attempt >= this.maxRetries) {
                        console.error('AndroidInterface: Max retries reached for showPayment');
                        alert('Payment interface not available. Please try again.');
                        return;
                    }
                    
                    setTimeout(function() {
                        if (self.isAvailable()) {
                            console.log('Android.showPayment now available after ' + attempt + ' retries');
                            try {
                                window.Android.showPayment();
                            } catch (e) {
                                console.error('Error calling Android.showPayment after retry:', e);
                            }
                        } else {
                            console.log('Retry attempt ' + (attempt + 1) + ' - Android.showPayment still not available');
                            self.retryShowPayment(attempt + 1);
                        }
                    }, this.retryDelay * Math.pow(1.5, attempt)); // Exponential backoff
                },
                
                // Safe wrapper for other Android interface methods
                callMethod: function(methodName, ...args) {
                    if (typeof window.Android !== 'undefined' && 
                        typeof window.Android[methodName] === 'function') {
                        try {
                            return window.Android[methodName](...args);
                        } catch (e) {
                            return false;
                        }
                    } else {
                        console.warn('Android.' + methodName + ' is not available');
                        return false;
                    }
                },
            };
            
            // Perform initial status check
            console.log('AndroidInterface polyfill loaded');
            if (window.AndroidInterface.isAvailable()) {
                console.log('✅ Android.showPayment is immediately available');
            } else {
                console.log('⚠️ Android.showPayment not yet available - retry logic will be used');
                window.AndroidInterface.debugStatus();
            }
        })();
    """.trimIndent()

    Log.d(TAG, "Injecting Android interface polyfill")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "Android interface polyfill result: $result")
    }
}

/**
 * Verifies that the Android interface is properly loaded
 */
fun verifyAndroidInterface(webView: WebView) {
    val js = """
        (function() {
            const status = {
                androidExists: typeof window.Android !== 'undefined',
                showPaymentExists: typeof window.Android !== 'undefined' && typeof window.Android.showPayment === 'function',
                polyfillExists: typeof window.AndroidInterface !== 'undefined'
            };
            console.log('Android Interface Verification:', JSON.stringify(status));
            return JSON.stringify(status);
        })();
    """.trimIndent()

    Log.d(TAG, "Verifying Android interface availability")
    webView.evaluateJavascript(js) { result ->
        Log.d(TAG, "Android interface verification result: $result")
    }
}

fun injectTheme(webView: WebView, theme: Int, mode: Int) {
    val js = """
        (function() {
            function getLocalId() {
                try {
                    const url = window.location.href;
                    const pathName = new URL(url).pathname;
                    const match = pathName.match(/\/u\/(\d+)\//);
                    return match ? match[1] : null;
                } catch {
                    return null;
                }
            }
            
            const localId = getLocalId();
            const key = `lumo-settings${"$"}{localId === null ? '' : `:${"$"}{localId}`}`;
            
            const themeObj = {
                theme: $theme,  // Int
                mode: $mode     // Int
            };

            localStorage.setItem(key, JSON.stringify(themeObj));
        })();
    """.trimIndent()

    webView.evaluateJavascript(js, null)
}

fun themeChangeListener(webView: WebView) {
    val js = """
        (function() {
            let observedButtons = new WeakSet();

            function extractThemeFromAriaLabel(ariaLabel) {
                // "Use Light theme" -> "Light"
                // "Use Dark theme" -> "Dark"
                // "Use System theme" -> "System"
                const match = ariaLabel?.match(/Use (\w+) theme/i);
                return match ? match[1] : null;
            }

            function attachClickObserver(button) {
                if (!button || observedButtons.has(button)) return;

                observedButtons.add(button);
                const ariaLabel = button.getAttribute("aria-label");
                const theme = extractThemeFromAriaLabel(ariaLabel);

                window.Android?.log("✅ Observing theme button: " + ariaLabel);

                button.addEventListener('click', function() {
                    const clickedTheme = extractThemeFromAriaLabel(this.getAttribute("aria-label"));
                    window.Android?.log("🎯 Theme button clicked: " + clickedTheme);
                    window.Android?.onThemeChanged(clickedTheme);
                });
            }

            function findAndObserveThemeButtons() {
                // Find all theme card buttons
                const buttons = document.querySelectorAll('button.lumo-theme-card-button');

                if (buttons.length > 0) {
                    window.Android?.log("Found " + buttons.length + " theme buttons");
                    buttons.forEach(attachClickObserver);
                }
            }

            // Body observer to detect when theme buttons appear
            const bodyObserver = new MutationObserver(mutations => {
                for (const mutation of mutations) {
                    for (const node of mutation.addedNodes) {
                        if (!(node instanceof HTMLElement)) continue;

                        // Case 1: the node itself is a theme button
                        if (node.matches("button.lumo-theme-card-button")) {
                            attachClickObserver(node);
                        }

                        // Case 2: theme buttons are inside the added node
                        const buttons = node.querySelectorAll("button.lumo-theme-card-button");
                        buttons.forEach(attachClickObserver);
                    }
                }
            });

            // Initial scan for existing buttons
            findAndObserveThemeButtons();

            bodyObserver.observe(document.body, {
                childList: true,
                subtree: true
            });

            window.Android?.log("Theme change listener initialized");
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}


fun keyboardHeightChange(isVisible: Boolean, keyboardHeight: Int): String =
    """(function() {
        if (window.onNativeKeyboardChange) {
            window.onNativeKeyboardChange($isVisible, $keyboardHeight);
            return true;
        } else {
            console.error('❌ window.onNativeKeyboardChange not found!');
            return false;
        }
    })();
    """.trimIndent()
