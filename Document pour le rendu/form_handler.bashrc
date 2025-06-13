#!/bin/bash

# Chemin vers le fichier de stockage des données
DATA_FILE="./data.txt" # Changez ceci si besoin, par exemple /tmp/var/data/myweb/data.txt

# Initialiser le contenu de la page HTML
HTML_CONTENT="Content-type: text/html\n\n<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>Réponse du Serveur</title></head><body>"

# Parser les arguments passés par le serveur Java
user_name=""
user_mail=""
for arg in "$@"; do
    if [ "$arg" == user_name=* ]; then
        user_name=$(echo "$arg" | cut -d'=' -f2-)
        user_name=$(echo "$user_name" | sed 's/%20/ /g') # Décodage simple des espaces
        user_name=$(echo "$user_name" | perl -MURI::Escape -ne 'print uri_unescape($_)') # Décodage complet
    elif [ "$arg" == user_mail=* ]; then
        user_mail=$(echo "$arg" | cut -d'=' -f2-)
        user_mail=$(echo "$user_mail" | sed 's/%40/@/g' | sed 's/%2E/./g') # Décodage simple des @ et .
        user_mail=$(echo "$user_mail" | perl -MURI::Escape -ne 'print uri_unescape($_)') # Décodage complet
    fi
done

# Décodage URL pour les valeurs
user_name=$(echo "$user_name" | sed 's/+/ /g; s/%/\\x/g' | xargs -0 printf %b 2>/dev/null) # Décodage avancé
user_mail=$(echo "$user_mail" | sed 's/+/ /g; s/%/\\x/g' | xargs -0 printf %b 2>/dev/null) # Décodage avancé


# Vérifier l'action et générer la réponse HTML
if [ -n "$user_name" ] && [ -n "$user_mail" ]; then
    # Les deux sont fournis : enregistrer
    echo "$user_name,$user_mail" >> "$DATA_FILE"
    HTML_CONTENT+="<h1>Enregistrement Réussi !</h1>"
    HTML_CONTENT+="<p>Nom : $user_name</p>"
    HTML_CONTENT+="<p>Email : $user_mail</p>"
    HTML_CONTENT+="<p>Vos données ont été enregistrées.</p>"
    HTML_CONTENT+="<button onclick=\"window.location.href='/formulaire.html'\">Saisir un autre nom</button>"
    HTML_CONTENT+="<button onclick=\"window.location.href='/'\">Retour à l'accueil</button>"
elif [ -n "$user_name" ] && [ -z "$user_mail" ]; then
    # Seul le nom est fourni : afficher les informations si déjà enregistré
    if grep -q "^$user_name," "$DATA_FILE"; then
        read_mail=$(grep "^$user_name," "$DATA_FILE" | head -n 1 | cut -d',' -f2)
        HTML_CONTENT+="<h1>Informations de l'utilisateur</h1>"
        HTML_CONTENT+="<p>Nom : $user_name</p>"
        HTML_CONTENT+="<p>Email enregistré : $read_mail</p>"
        HTML_CONTENT+="<button onclick=\"window.location.href='/formulaire.html'\">Saisir un autre nom</button>"
        HTML_CONTENT+="<button onclick=\"window.location.href='/'\">Retour à l'accueil</button>"
    else
        HTML_CONTENT+="<h1>Erreur : Nom non enregistré</h1>"
        HTML_CONTENT+="<p>Le nom '$user_name' n'est pas trouvé dans nos enregistrements.</p>"
        HTML_CONTENT+="<button onclick=\"window.location.href='/formulaire.html'\">Retour au formulaire</button>"
    fi
else
    # Ni nom ni email, ou cas invalide
    HTML_CONTENT+="<h1>Erreur de formulaire</h1>"
    HTML_CONTENT+="<p>Veuillez fournir au moins un nom.</p>"
    HTML_CONTENT+="<button onclick=\"window.location.href='/formulaire.html'\">Retour au formulaire</button>"
fi

HTML_CONTENT+="</body></html>"

echo -e "$HTML_CONTENT"
