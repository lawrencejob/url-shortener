"use client"

import { useState } from "react"
import { Button } from "./ui/button"
import { Field, FieldLabel, FieldDescription, FieldError } from "./ui/field"
import { Input } from "./ui/input"
import { useActionState } from "react"
import { shortenAction } from "@/app/actions"
import { Spinner } from "./ui/spinner"
import { IconPlus, IconSend, IconX } from "@tabler/icons-react"

export default function Form() {
    const [state, formAction, pending] = useActionState(shortenAction, undefined)
    const [hasAlias, setHasAlias] = useState(false)

    return (
        <form
            action={formAction}
            className="space-y-6 max-w-md mx-auto p-6 rounded-2xl border bg-background"
        >
            <Field>
                <FieldLabel>URL to shorten</FieldLabel>
                <Input
                    placeholder="https://example.com"
                    name="fullUrl"
                    required
                />
                <FieldError>{state?.errors?.properties?.fullUrl?.errors.map((error, index) => (
                    <div key={index}>{error}</div>
                ))}</FieldError>
            </Field>

            {(hasAlias || !!state?.errors?.properties?.alias) ? (
                <Field>
                    <FieldLabel>Custom alias</FieldLabel>
                    <div className="flex flex-row gap-4 align-bottom">
                    <Input
                        placeholder="e.g. mylink123"
                        name="alias"
                    />
                    <Button
                        type="button"
                        variant="ghost"
                        onClick={() => setHasAlias(false)}
                        className="text-sm mt-1 flex-grow-0"
                    ><IconX /> Remove alias </Button>
                    </div>
                    <FieldDescription>
                        3-32 letters or numbers
                    </FieldDescription>
                    <FieldError>{state?.errors?.properties?.alias?.errors.map((error, index) => (
                        <div key={index}>{error}</div>
                    ))}</FieldError>
                </Field>
            ) : (
                <Button
                    type="button"
                    variant="secondary"
                    onClick={() => setHasAlias(true)}
                >
                    <IconPlus /> Add custom alias
                </Button>
            )}

            <Button type="submit" className="w-full" disabled={pending}>
                {pending ? <Spinner /> : <><IconSend /> Shorten URL</>}
            </Button>

            {state?.shortUrl && (
                <p className="text-sm mt-4">
                    Short URL:{" "}
                    <a href={state.shortUrl} className="underline text-blue-600">
                        {state.shortUrl}
                    </a>
                </p>
            )}
        </form>
    )
}